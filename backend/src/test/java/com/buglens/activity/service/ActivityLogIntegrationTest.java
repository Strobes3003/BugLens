package com.buglens.activity.service;

import com.buglens.activity.entity.ActivityAction;
import com.buglens.activity.entity.ActivityLog;
import com.buglens.activity.repository.ActivityLogRepository;
import com.buglens.auth.entity.User;
import com.buglens.auth.repository.UserRepository;
import com.buglens.comment.dto.request.CreateCommentRequest;
import com.buglens.comment.repository.CommentRepository;
import com.buglens.comment.service.CommentService;
import com.buglens.component.entity.Component;
import com.buglens.component.entity.ComponentStatus;
import com.buglens.component.repository.ComponentRepository;
import com.buglens.issue.dto.request.CreateIssueRequest;
import com.buglens.issue.dto.request.UpdateIssueRequest;
import com.buglens.issue.dto.response.IssueResponse;
import com.buglens.issue.entity.IssuePriority;
import com.buglens.issue.entity.IssueSeverity;
import com.buglens.issue.entity.IssueStatus;
import com.buglens.issue.repository.IssueRepository;
import com.buglens.issue.service.IssueService;
import com.buglens.project.entity.Project;
import com.buglens.project.entity.ProjectStatus;
import com.buglens.project.repository.ProjectRepository;
import com.buglens.workflow.dto.request.TransitionIssueRequest;
import com.buglens.workflow.service.WorkflowService;
import com.buglens.workspace.entity.Workspace;
import com.buglens.workspace.entity.WorkspaceMember;
import com.buglens.workspace.entity.WorkspaceRole;
import com.buglens.workspace.repository.WorkspaceMemberRepository;
import com.buglens.workspace.repository.WorkspaceRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * End-to-end proof that domain events reach the activity log after the originating transaction
 * commits.
 *
 * <p>Deliberately NOT annotated {@code @Transactional}: the test-managed transaction would roll
 * back and never commit, so an {@code AFTER_COMMIT} listener would never fire and this test would
 * pass or fail for entirely the wrong reason. Data is therefore cleaned up explicitly.
 */
@SpringBootTest
class ActivityLogIntegrationTest {

    @Autowired
    private IssueService issueService;

    @Autowired
    private WorkflowService workflowService;

    @Autowired
    private CommentService commentService;

    @Autowired
    private ActivityLogRepository activityLogRepository;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private IssueRepository issueRepository;

    @Autowired
    private ComponentRepository componentRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private WorkspaceMemberRepository workspaceMemberRepository;

    @Autowired
    private WorkspaceRepository workspaceRepository;

    @Autowired
    private UserRepository userRepository;

    private Long actorId;
    private Long componentId;

    @BeforeEach
    void setUp() {
        cleanUp();

        User actor = userRepository.save(
                new User("Actor", "actor-" + System.nanoTime() + "@buglens.test", "hash", null)
        );
        Workspace workspace = workspaceRepository.save(
                new Workspace("Workspace", "ws-" + System.nanoTime(), actor)
        );
        workspaceMemberRepository.save(new WorkspaceMember(workspace, actor, WorkspaceRole.OWNER));
        Project project = projectRepository.save(
                new Project(workspace, "BugLens", "BL", null, ProjectStatus.ACTIVE)
        );
        Component component = componentRepository.save(
                new Component(project, "Auth", null, ComponentStatus.ACTIVE)
        );

        actorId = actor.getId();
        componentId = component.getId();
    }

    @AfterEach
    void tearDown() {
        cleanUp();
    }

    private void cleanUp() {
        activityLogRepository.deleteAll();
        commentRepository.deleteAll();
        issueRepository.deleteAll();
        componentRepository.deleteAll();
        projectRepository.deleteAll();
        workspaceMemberRepository.deleteAll();
        workspaceRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void creatingAnIssueRecordsCreationActivity() {
        IssueResponse issue = createIssue("Login fails");

        List<ActivityLog> logs = activityLogRepository.findAllByIssueIdOrderByCreatedAtAsc(issue.id());

        assertEquals(1, logs.size());
        assertEquals(ActivityAction.ISSUE_CREATED, logs.get(0).getActionType());
        assertEquals("created issue " + issue.issueKey(), logs.get(0).getDescription());
        assertEquals(actorId, logs.get(0).getActor().getId());
    }

    @Test
    void transitioningRecordsStatusChangeActivity() {
        IssueResponse issue = createIssue("Login fails");

        workflowService.transition(
                issue.id(), new TransitionIssueRequest(IssueStatus.IN_PROGRESS, null), actorId
        );

        List<ActivityLog> logs = activityLogRepository.findAllByIssueIdOrderByCreatedAtAsc(issue.id());

        assertEquals(2, logs.size());
        assertEquals(ActivityAction.STATUS_CHANGED, logs.get(1).getActionType());
        assertEquals("changed status from OPEN to IN_PROGRESS", logs.get(1).getDescription());
    }

    @Test
    void commentingRecordsCommentActivity() {
        IssueResponse issue = createIssue("Login fails");

        commentService.addComment(issue.id(), new CreateCommentRequest("Reproduced"), actorId);

        List<ActivityLog> logs = activityLogRepository.findAllByIssueIdOrderByCreatedAtAsc(issue.id());

        assertEquals(2, logs.size());
        assertEquals(ActivityAction.COMMENT_ADDED, logs.get(1).getActionType());
        assertEquals("added a comment", logs.get(1).getDescription());
    }

    @Test
    void updatingRecordsChangedFieldNames() {
        IssueResponse issue = createIssue("Login fails");

        issueService.updateDetails(
                issue.id(),
                new UpdateIssueRequest("Renamed", null, IssuePriority.LOW, null, null, null, null, null, null),
                actorId
        );

        List<ActivityLog> logs = activityLogRepository.findAllByIssueIdOrderByCreatedAtAsc(issue.id());

        assertEquals(2, logs.size());
        assertEquals(ActivityAction.ISSUE_UPDATED, logs.get(1).getActionType());
        assertEquals("updated title, priority", logs.get(1).getDescription());
    }

    @Test
    void transitionWithCommentRecordsBothActivities() {
        IssueResponse issue = createIssue("Login fails");

        workflowService.transition(
                issue.id(),
                new TransitionIssueRequest(IssueStatus.IN_PROGRESS, "Picking this up"),
                actorId
        );

        List<ActivityLog> logs = activityLogRepository.findAllByIssueIdOrderByCreatedAtAsc(issue.id());

        assertEquals(3, logs.size());
        assertTrue(logs.stream().anyMatch(l -> l.getActionType() == ActivityAction.COMMENT_ADDED));
        assertTrue(logs.stream().anyMatch(l -> l.getActionType() == ActivityAction.STATUS_CHANGED));
    }

    @Test
    void rolledBackTransitionRecordsNothing() {
        IssueResponse issue = createIssue("Login fails");
        long before = activityLogRepository.countByIssueId(issue.id());

        try {
            workflowService.transition(
                    issue.id(), new TransitionIssueRequest(IssueStatus.CLOSED, null), actorId
            );
        } catch (RuntimeException expected) {
            // OPEN -> CLOSED is rejected by the workflow engine.
        }

        assertEquals(before, activityLogRepository.countByIssueId(issue.id()));
    }

    @Test
    void listForIssueReturnsHistoryChronologically() {
        IssueResponse issue = createIssue("Login fails");
        workflowService.transition(
                issue.id(), new TransitionIssueRequest(IssueStatus.IN_PROGRESS, null), actorId
        );

        var history = activityLogRepository.findAllByIssueIdOrderByCreatedAtAsc(issue.id());

        assertEquals(ActivityAction.ISSUE_CREATED, history.get(0).getActionType());
        assertEquals(ActivityAction.STATUS_CHANGED, history.get(1).getActionType());
    }

    private IssueResponse createIssue(String title) {
        return issueService.create(
                new CreateIssueRequest(
                        title, null, IssuePriority.HIGH, IssueSeverity.HIGH, componentId, null, null
                ),
                actorId
        );
    }
}
