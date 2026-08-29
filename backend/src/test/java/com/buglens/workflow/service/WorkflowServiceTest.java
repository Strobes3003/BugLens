package com.buglens.workflow.service;

import com.buglens.auth.entity.User;
import com.buglens.comment.service.CommentService;
import com.buglens.component.entity.Component;
import com.buglens.issue.dto.response.IssueResponse;
import com.buglens.issue.entity.Issue;
import com.buglens.issue.entity.IssuePriority;
import com.buglens.issue.entity.IssueSeverity;
import com.buglens.issue.entity.IssueStatus;
import com.buglens.issue.exception.IssueAccessDeniedException;
import com.buglens.issue.exception.IssueNotFoundException;
import com.buglens.issue.repository.IssueRepository;
import com.buglens.project.entity.Project;
import com.buglens.release.entity.Release;
import com.buglens.workflow.dto.request.TransitionIssueRequest;
import com.buglens.workflow.dto.response.AllowedTransitionsResponse;
import com.buglens.workflow.exception.InvalidStateTransitionException;
import com.buglens.workspace.entity.Workspace;
import com.buglens.workspace.exception.WorkspaceAccessDeniedException;
import com.buglens.workspace.service.WorkspaceAccessService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class WorkflowServiceTest {

    private static final Long ISSUE_ID = 5L;
    private static final Long ACTOR_ID = 7L;
    private static final Long WORKSPACE_ID = 3L;

    @Mock
    private IssueRepository issueRepository;

    @Mock
    private WorkspaceAccessService workspaceAccessService;

    @Mock
    private CommentService commentService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private Workspace workspace;

    @Mock
    private Project project;

    @Mock
    private Component component;

    @Mock
    private Release release;

    private WorkflowService workflowService;
    private User reporter;

    @BeforeEach
    void setUp() {
        workflowService = new WorkflowService(issueRepository, workspaceAccessService, commentService, eventPublisher);
        reporter = new User("Reporter", "reporter@buglens.test", "hash", null);

        lenient().when(workspace.getId()).thenReturn(WORKSPACE_ID);
        lenient().when(project.getId()).thenReturn(1L);
        lenient().when(project.getKey()).thenReturn("BL");
        lenient().when(project.getName()).thenReturn("BugLens");
        lenient().when(project.getWorkspace()).thenReturn(workspace);
        lenient().when(component.getId()).thenReturn(10L);
        lenient().when(component.getName()).thenReturn("Authentication");
        lenient().when(component.getProject()).thenReturn(project);
        lenient().when(release.getId()).thenReturn(20L);
        lenient().when(release.getName()).thenReturn("v2.4");
    }

    @ParameterizedTest(name = "{0} -> {1} is allowed")
    @CsvSource({
            "OPEN,        IN_PROGRESS",
            "IN_PROGRESS, IN_REVIEW",
            "IN_PROGRESS, OPEN",
            "IN_REVIEW,   RESOLVED",
            "IN_REVIEW,   IN_PROGRESS",
            "RESOLVED,    CLOSED",
            "RESOLVED,    IN_PROGRESS",
            "CLOSED,      OPEN",
            "CLOSED,      IN_PROGRESS"
    })
    void appliesEveryLegalTransition(IssueStatus from, IssueStatus to) {
        Issue issue = issueWithStatus(from);
        when(issueRepository.findById(ISSUE_ID)).thenReturn(Optional.of(issue));

        IssueResponse response = workflowService.transition(
                ISSUE_ID, new TransitionIssueRequest(to, null), ACTOR_ID
        );

        assertEquals(to, issue.getStatus());
        assertEquals(to, response.status());
    }

    @ParameterizedTest(name = "{0} -> {1} is rejected")
    @CsvSource({
            "OPEN,        RESOLVED",
            "OPEN,        CLOSED",
            "OPEN,        IN_REVIEW",
            "OPEN,        OPEN",
            "IN_PROGRESS, RESOLVED",
            "IN_PROGRESS, CLOSED",
            "IN_PROGRESS, IN_PROGRESS",
            "IN_REVIEW,   OPEN",
            "IN_REVIEW,   CLOSED",
            "RESOLVED,    OPEN",
            "RESOLVED,    IN_REVIEW",
            "CLOSED,      RESOLVED",
            "CLOSED,      IN_REVIEW",
            "CLOSED,      CLOSED"
    })
    void rejectsEveryIllegalTransition(IssueStatus from, IssueStatus to) {
        Issue issue = issueWithStatus(from);
        when(issueRepository.findById(ISSUE_ID)).thenReturn(Optional.of(issue));

        assertThrows(
                InvalidStateTransitionException.class,
                () -> workflowService.transition(ISSUE_ID, new TransitionIssueRequest(to, null), ACTOR_ID)
        );
        assertEquals(from, issue.getStatus());
    }

    @Test
    void rejectedTransitionReportsAllowedTargets() {
        Issue issue = issueWithStatus(IssueStatus.OPEN);
        when(issueRepository.findById(ISSUE_ID)).thenReturn(Optional.of(issue));

        InvalidStateTransitionException exception = assertThrows(
                InvalidStateTransitionException.class,
                () -> workflowService.transition(
                        ISSUE_ID, new TransitionIssueRequest(IssueStatus.RESOLVED, null), ACTOR_ID
                )
        );

        assertEquals(List.of(IssueStatus.IN_PROGRESS), exception.getAllowedTransitions());
    }

    @ParameterizedTest(name = "allowed from {0}")
    @CsvSource({
            "OPEN,        IN_PROGRESS",
            "IN_PROGRESS, OPEN|IN_REVIEW",
            "IN_REVIEW,   IN_PROGRESS|RESOLVED",
            "RESOLVED,    IN_PROGRESS|CLOSED",
            "CLOSED,      OPEN|IN_PROGRESS"
    })
    void returnsAllowedTransitionsForCurrentStatus(IssueStatus from, String expected) {
        Issue issue = issueWithStatus(from);
        when(issueRepository.findById(ISSUE_ID)).thenReturn(Optional.of(issue));

        AllowedTransitionsResponse response = workflowService.getAllowedTransitions(ISSUE_ID, ACTOR_ID);

        List<IssueStatus> expectedTargets = java.util.Arrays.stream(expected.split("\\|"))
                .map(IssueStatus::valueOf)
                .toList();

        assertEquals(from, response.currentStatus());
        assertEquals(expectedTargets, response.allowedTransitions());
    }

    @Test
    void transitionPersistsSuppliedCommentAsIssueComment() {
        Issue issue = issueWithStatus(IssueStatus.OPEN);
        when(issueRepository.findById(ISSUE_ID)).thenReturn(Optional.of(issue));

        workflowService.transition(
                ISSUE_ID,
                new TransitionIssueRequest(IssueStatus.IN_PROGRESS, "  Picking this up  "),
                ACTOR_ID
        );

        org.mockito.ArgumentCaptor<com.buglens.comment.dto.request.CreateCommentRequest> captor =
                org.mockito.ArgumentCaptor.forClass(com.buglens.comment.dto.request.CreateCommentRequest.class);
        verify(commentService).addComment(org.mockito.ArgumentMatchers.eq(ISSUE_ID), captor.capture(),
                org.mockito.ArgumentMatchers.eq(ACTOR_ID));
        assertEquals("  Picking this up  ", captor.getValue().body());
    }

    @Test
    void transitionWithoutCommentPersistsNothing() {
        Issue issue = issueWithStatus(IssueStatus.OPEN);
        when(issueRepository.findById(ISSUE_ID)).thenReturn(Optional.of(issue));

        workflowService.transition(ISSUE_ID, new TransitionIssueRequest(IssueStatus.IN_PROGRESS, null), ACTOR_ID);
        workflowService.transition(ISSUE_ID, new TransitionIssueRequest(IssueStatus.OPEN, "   "), ACTOR_ID);

        verify(commentService, org.mockito.Mockito.never())
                .addComment(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any());
    }

    @Test
    void rejectedTransitionPersistsNoComment() {
        Issue issue = issueWithStatus(IssueStatus.OPEN);
        when(issueRepository.findById(ISSUE_ID)).thenReturn(Optional.of(issue));

        assertThrows(
                InvalidStateTransitionException.class,
                () -> workflowService.transition(
                        ISSUE_ID, new TransitionIssueRequest(IssueStatus.CLOSED, "should not persist"), ACTOR_ID
                )
        );

        verify(commentService, org.mockito.Mockito.never())
                .addComment(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any());
    }

    @Test
    void transitionPublishesStatusChangedEventWithBothStatuses() {
        Issue issue = issueWithStatus(IssueStatus.OPEN);
        when(issueRepository.findById(ISSUE_ID)).thenReturn(Optional.of(issue));

        workflowService.transition(
                ISSUE_ID, new TransitionIssueRequest(IssueStatus.IN_PROGRESS, null), ACTOR_ID
        );

        org.mockito.ArgumentCaptor<com.buglens.event.domain.IssueStatusChangedEvent> captor =
                org.mockito.ArgumentCaptor.forClass(com.buglens.event.domain.IssueStatusChangedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertEquals(IssueStatus.OPEN, captor.getValue().oldStatus());
        assertEquals(IssueStatus.IN_PROGRESS, captor.getValue().newStatus());
        assertEquals(ACTOR_ID, captor.getValue().actorId());
    }

    @Test
    void rejectedTransitionPublishesNoEvent() {
        Issue issue = issueWithStatus(IssueStatus.OPEN);
        when(issueRepository.findById(ISSUE_ID)).thenReturn(Optional.of(issue));

        assertThrows(
                InvalidStateTransitionException.class,
                () -> workflowService.transition(
                        ISSUE_ID, new TransitionIssueRequest(IssueStatus.CLOSED, null), ACTOR_ID
                )
        );

        verify(eventPublisher, org.mockito.Mockito.never())
                .publishEvent(org.mockito.ArgumentMatchers.any(Object.class));
    }

    @Test
    void transitionThrowsWhenIssueMissing() {
        when(issueRepository.findById(404L)).thenReturn(Optional.empty());

        assertThrows(
                IssueNotFoundException.class,
                () -> workflowService.transition(
                        404L, new TransitionIssueRequest(IssueStatus.IN_PROGRESS, null), ACTOR_ID
                )
        );
    }

    @Test
    void allowedTransitionsThrowsWhenIssueMissing() {
        when(issueRepository.findById(404L)).thenReturn(Optional.empty());

        assertThrows(
                IssueNotFoundException.class,
                () -> workflowService.getAllowedTransitions(404L, ACTOR_ID)
        );
    }

    @Test
    void transitionRequiresWorkspaceMembership() {
        Issue issue = issueWithStatus(IssueStatus.OPEN);
        when(issueRepository.findById(ISSUE_ID)).thenReturn(Optional.of(issue));
        doThrow(new WorkspaceAccessDeniedException())
                .when(workspaceAccessService).requireMember(WORKSPACE_ID, ACTOR_ID);

        assertThrows(
                IssueAccessDeniedException.class,
                () -> workflowService.transition(
                        ISSUE_ID, new TransitionIssueRequest(IssueStatus.IN_PROGRESS, null), ACTOR_ID
                )
        );
        assertEquals(IssueStatus.OPEN, issue.getStatus());
    }

    private Issue issueWithStatus(IssueStatus status) {
        return new Issue(
                "BL-1",
                "Existing issue",
                "Description",
                status,
                IssuePriority.HIGH,
                IssueSeverity.HIGH,
                component,
                release,
                reporter,
                null
        );
    }
}
