package com.buglens.issue.service;

import com.buglens.auth.entity.User;
import com.buglens.auth.repository.UserRepository;
import com.buglens.component.entity.Component;
import com.buglens.component.repository.ComponentRepository;
import com.buglens.issue.dto.request.CreateIssueRequest;
import com.buglens.issue.dto.request.UpdateIssueRequest;
import com.buglens.issue.dto.response.IssueResponse;
import com.buglens.issue.entity.Issue;
import com.buglens.issue.entity.IssuePriority;
import com.buglens.issue.entity.IssueSeverity;
import com.buglens.issue.entity.IssueStatus;
import com.buglens.issue.exception.CrossProjectIssueException;
import com.buglens.issue.exception.IssueAccessDeniedException;
import com.buglens.issue.exception.IssueComponentNotFoundException;
import com.buglens.issue.exception.IssueNotFoundException;
import com.buglens.issue.repository.IssueRepository;
import com.buglens.project.entity.Project;
import com.buglens.project.repository.ProjectRepository;
import com.buglens.release.entity.Release;
import com.buglens.release.repository.ReleaseRepository;
import com.buglens.workspace.entity.Workspace;
import com.buglens.workspace.exception.WorkspaceAccessDeniedException;
import com.buglens.workspace.service.WorkspaceAccessService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IssueServiceTest {

    private static final Long ACTOR_ID = 7L;
    private static final Long PROJECT_ID = 1L;
    private static final Long OTHER_PROJECT_ID = 2L;
    private static final Long WORKSPACE_ID = 3L;

    @Mock
    private IssueRepository issueRepository;

    @Mock
    private ComponentRepository componentRepository;

    @Mock
    private ReleaseRepository releaseRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private IssueKeyGenerator issueKeyGenerator;

    @Mock
    private WorkspaceAccessService workspaceAccessService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private Workspace workspace;

    @Mock
    private Project project;

    @Mock
    private Project otherProject;

    @Mock
    private Component component;

    @Mock
    private Release release;

    private IssueService issueService;
    private User reporter;

    @BeforeEach
    void setUp() {
        issueService = new IssueService(
                issueRepository,
                componentRepository,
                releaseRepository,
                projectRepository,
                userRepository,
                issueKeyGenerator,
                workspaceAccessService,
                eventPublisher
        );

        reporter = new User("Reporter", "reporter@buglens.test", "hash", null);

        lenient().when(workspace.getId()).thenReturn(WORKSPACE_ID);
        lenient().when(project.getId()).thenReturn(PROJECT_ID);
        lenient().when(project.getWorkspace()).thenReturn(workspace);
        lenient().when(project.getKey()).thenReturn("BL");
        lenient().when(project.getName()).thenReturn("BugLens");
        lenient().when(otherProject.getId()).thenReturn(OTHER_PROJECT_ID);
        lenient().when(component.getId()).thenReturn(10L);
        lenient().when(component.getName()).thenReturn("Authentication");
        lenient().when(component.getProject()).thenReturn(project);
        lenient().when(release.getId()).thenReturn(20L);
        lenient().when(release.getName()).thenReturn("v2.4");
        lenient().when(release.getProject()).thenReturn(project);
    }

    @Test
    void createsIssueWithGeneratedKey() {
        when(componentRepository.findById(10L)).thenReturn(Optional.of(component));
        when(releaseRepository.findById(20L)).thenReturn(Optional.of(release));
        when(userRepository.findById(ACTOR_ID)).thenReturn(Optional.of(reporter));
        when(issueKeyGenerator.nextKey(project)).thenReturn("BL-13");
        when(issueRepository.save(any(Issue.class))).thenAnswer(call -> call.getArgument(0));

        CreateIssueRequest request = new CreateIssueRequest(
                "  Login fails after password reset  ",
                "  Steps attached.  ",
                IssuePriority.HIGH,
                IssueSeverity.CRITICAL,
                10L,
                20L,
                null
        );

        IssueResponse response = issueService.create(request, ACTOR_ID);

        ArgumentCaptor<Issue> captor = ArgumentCaptor.forClass(Issue.class);
        verify(issueRepository).save(captor.capture());
        Issue saved = captor.getValue();

        assertEquals("BL-13", saved.getIssueKey());
        assertEquals("Login fails after password reset", saved.getTitle());
        assertEquals("Steps attached.", saved.getDescription());
        assertEquals(IssueStatus.OPEN, saved.getStatus());
        assertEquals(reporter, saved.getReporter());
        assertEquals("BL-13", response.issueKey());
        assertEquals(IssueStatus.OPEN, response.status());
    }

    @Test
    void createsBacklogIssueWhenReleaseOmitted() {
        when(componentRepository.findById(10L)).thenReturn(Optional.of(component));
        when(userRepository.findById(ACTOR_ID)).thenReturn(Optional.of(reporter));
        when(issueKeyGenerator.nextKey(project)).thenReturn("BL-14");
        when(issueRepository.save(any(Issue.class))).thenAnswer(call -> call.getArgument(0));

        IssueResponse response = issueService.create(
                new CreateIssueRequest("Backlog item", null, IssuePriority.LOW, IssueSeverity.LOW, 10L, null, null),
                ACTOR_ID
        );

        assertNull(response.releaseId());
        verify(releaseRepository, never()).findById(any());
    }

    @Test
    void rejectsCreateWhenReleaseBelongsToAnotherProject() {
        when(componentRepository.findById(10L)).thenReturn(Optional.of(component));
        when(release.getProject()).thenReturn(otherProject);
        when(releaseRepository.findById(20L)).thenReturn(Optional.of(release));

        CreateIssueRequest request = new CreateIssueRequest(
                "Cross project", null, IssuePriority.HIGH, IssueSeverity.HIGH, 10L, 20L, null
        );

        assertThrows(CrossProjectIssueException.class, () -> issueService.create(request, ACTOR_ID));
        verify(issueRepository, never()).save(any());
    }

    @Test
    void rejectsCreateWhenComponentMissing() {
        when(componentRepository.findById(99L)).thenReturn(Optional.empty());

        CreateIssueRequest request = new CreateIssueRequest(
                "No component", null, IssuePriority.HIGH, IssueSeverity.HIGH, 99L, null, null
        );

        assertThrows(IssueComponentNotFoundException.class, () -> issueService.create(request, ACTOR_ID));
    }

    @Test
    void rejectsCreateWhenActorIsNotWorkspaceMember() {
        when(componentRepository.findById(10L)).thenReturn(Optional.of(component));
        doThrow(new WorkspaceAccessDeniedException())
                .when(workspaceAccessService).requireMember(WORKSPACE_ID, ACTOR_ID);

        CreateIssueRequest request = new CreateIssueRequest(
                "Denied", null, IssuePriority.HIGH, IssueSeverity.HIGH, 10L, null, null
        );

        assertThrows(IssueAccessDeniedException.class, () -> issueService.create(request, ACTOR_ID));
        verify(issueRepository, never()).save(any());
    }

    @Test
    void movesIssueToBacklogWhenReleaseCleared() {
        Issue issue = existingIssue();
        when(issueRepository.findById(5L)).thenReturn(Optional.of(issue));

        UpdateIssueRequest request = new UpdateIssueRequest(
                null, null, null, null, null, null, true, null, null
        );

        IssueResponse response = issueService.updateDetails(5L, request, ACTOR_ID);

        assertNull(issue.getRelease());
        assertNull(response.releaseId());
    }

    @Test
    void rejectsUpdateWhenComponentBelongsToAnotherProject() {
        Issue issue = existingIssue();
        Component foreignComponent = org.mockito.Mockito.mock(Component.class);
        when(foreignComponent.getProject()).thenReturn(otherProject);
        when(issueRepository.findById(5L)).thenReturn(Optional.of(issue));
        when(componentRepository.findById(11L)).thenReturn(Optional.of(foreignComponent));

        UpdateIssueRequest request = new UpdateIssueRequest(
                null, null, null, null, 11L, null, null, null, null
        );

        assertThrows(CrossProjectIssueException.class, () -> issueService.updateDetails(5L, request, ACTOR_ID));
    }

    @Test
    void updateDoesNotChangeStatusOrIssueKey() {
        Issue issue = existingIssue();
        when(issueRepository.findById(5L)).thenReturn(Optional.of(issue));

        UpdateIssueRequest request = new UpdateIssueRequest(
                "Renamed title", null, IssuePriority.LOW, IssueSeverity.LOW, null, null, null, null, null
        );

        IssueResponse response = issueService.updateDetails(5L, request, ACTOR_ID);

        assertEquals("BL-1", response.issueKey());
        assertEquals(IssueStatus.OPEN, response.status());
        assertEquals("Renamed title", response.title());
        assertEquals(IssuePriority.LOW, response.priority());
    }

    @Test
    void deleteRequiresManagerRole() {
        Issue issue = existingIssue();
        when(issueRepository.findById(5L)).thenReturn(Optional.of(issue));
        doThrow(new WorkspaceAccessDeniedException())
                .when(workspaceAccessService).requireMemberManager(WORKSPACE_ID, ACTOR_ID);

        assertThrows(IssueAccessDeniedException.class, () -> issueService.delete(5L, ACTOR_ID));
        verify(issueRepository, never()).delete(any());
    }

    @Test
    void createPublishesIssueCreatedEvent() {
        when(componentRepository.findById(10L)).thenReturn(Optional.of(component));
        when(userRepository.findById(ACTOR_ID)).thenReturn(Optional.of(reporter));
        when(issueKeyGenerator.nextKey(project)).thenReturn("BL-20");
        when(issueRepository.save(any(Issue.class))).thenAnswer(call -> call.getArgument(0));

        issueService.create(
                new CreateIssueRequest("New issue", null, IssuePriority.LOW, IssueSeverity.LOW, 10L, null, null),
                ACTOR_ID
        );

        ArgumentCaptor<com.buglens.event.domain.IssueCreatedEvent> captor =
                ArgumentCaptor.forClass(com.buglens.event.domain.IssueCreatedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertEquals("BL-20", captor.getValue().issueKey());
        assertEquals(ACTOR_ID, captor.getValue().actorId());
    }

    @Test
    void updatePublishesIssueUpdatedEventNamingChangedFields() {
        Issue issue = existingIssue();
        when(issueRepository.findById(5L)).thenReturn(Optional.of(issue));

        issueService.updateDetails(
                5L,
                new UpdateIssueRequest("Renamed", null, IssuePriority.LOW, null, null, null, null, null, null),
                ACTOR_ID
        );

        ArgumentCaptor<com.buglens.event.domain.IssueUpdatedEvent> captor =
                ArgumentCaptor.forClass(com.buglens.event.domain.IssueUpdatedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertEquals(java.util.List.of("title", "priority"), captor.getValue().changedFields());
    }

    @Test
    void updateWithNoActualChangePublishesNothing() {
        Issue issue = existingIssue();
        when(issueRepository.findById(5L)).thenReturn(Optional.of(issue));

        issueService.updateDetails(
                5L,
                new UpdateIssueRequest(null, null, null, null, null, null, null, null, null),
                ACTOR_ID
        );

        verify(eventPublisher, never()).publishEvent(any(Object.class));
    }

    @Test
    void rejectedCreatePublishesNoEvent() {
        when(componentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(IssueComponentNotFoundException.class, () -> issueService.create(
                new CreateIssueRequest("x", null, IssuePriority.LOW, IssueSeverity.LOW, 99L, null, null),
                ACTOR_ID
        ));

        verify(eventPublisher, never()).publishEvent(any(Object.class));
    }

    @Test
    void getByIdThrowsWhenMissing() {
        when(issueRepository.findById(404L)).thenReturn(Optional.empty());

        assertThrows(IssueNotFoundException.class, () -> issueService.getById(404L, ACTOR_ID));
    }

    private Issue existingIssue() {
        return new Issue(
                "BL-1",
                "Existing issue",
                "Description",
                IssueStatus.OPEN,
                IssuePriority.HIGH,
                IssueSeverity.HIGH,
                component,
                release,
                reporter,
                null
        );
    }
}
