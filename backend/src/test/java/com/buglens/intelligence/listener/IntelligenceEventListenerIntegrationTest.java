package com.buglens.intelligence.listener;

import com.buglens.auth.entity.User;
import com.buglens.auth.repository.UserRepository;
import com.buglens.component.entity.ComponentStatus;
import com.buglens.component.repository.ComponentRepository;
import com.buglens.dependency.repository.IssueDependencyRepository;
import com.buglens.dependency.service.DependencyService;
import com.buglens.event.domain.IssueStatusChangedEvent;
import com.buglens.intelligence.repository.ComponentHealthRepository;
import com.buglens.intelligence.repository.IssueImpactRepository;
import com.buglens.intelligence.repository.ReleaseRiskRepository;
import com.buglens.issue.dto.request.CreateIssueRequest;
import com.buglens.issue.entity.Issue;
import com.buglens.issue.entity.IssuePriority;
import com.buglens.issue.entity.IssueSeverity;
import com.buglens.issue.entity.IssueStatus;
import com.buglens.issue.repository.IssueRepository;
import com.buglens.issue.service.IssueService;
import com.buglens.project.entity.Project;
import com.buglens.project.entity.ProjectStatus;
import com.buglens.project.repository.ProjectRepository;
import com.buglens.release.entity.Release;
import com.buglens.release.entity.ReleaseStatus;
import com.buglens.release.repository.ReleaseRepository;
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
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Proves domain events actually drive intelligence recalculation.
 *
 * <p>Not {@code @Transactional}: an {@code AFTER_COMMIT} listener never fires inside a
 * test-managed transaction that rolls back, so the test would pass for the wrong reason. Data is
 * cleaned up explicitly instead.
 *
 * <p>Recalculation is asynchronous, so assertions poll for the expected value rather than
 * reading once. A fixed sleep would either be flaky or slow; polling with a deadline fails fast
 * when the wiring is broken and returns as soon as the background thread lands.
 */
@SpringBootTest
@Import(IntelligenceEventListenerIntegrationTest.TransactionalPublisher.class)
class IntelligenceEventListenerIntegrationTest {

    private static final long TIMEOUT_MILLIS = 5_000;
    private static final long POLL_MILLIS = 50;

    @Autowired private IssueService issueService;
    @Autowired private WorkflowService workflowService;
    @Autowired private DependencyService dependencyService;
    @Autowired private TransactionalPublisher transactionalPublisher;

    @Autowired private IssueImpactRepository issueImpactRepository;
    @Autowired private ComponentHealthRepository componentHealthRepository;
    @Autowired private ReleaseRiskRepository releaseRiskRepository;
    @Autowired private IssueDependencyRepository dependencyRepository;
    @Autowired private IssueRepository issueRepository;
    @Autowired private ComponentRepository componentRepository;
    @Autowired private ReleaseRepository releaseRepository;
    @Autowired private ProjectRepository projectRepository;
    @Autowired private WorkspaceMemberRepository workspaceMemberRepository;
    @Autowired private WorkspaceRepository workspaceRepository;
    @Autowired private UserRepository userRepository;

    private Long actorId;
    private Long componentId;
    private Long releaseId;

    /**
     * Publishes from inside a transaction. An {@code AFTER_COMMIT} listener needs a transaction
     * to commit, so publishing straight from the test would silently do nothing.
     */
    @TestConfiguration
    static class TransactionalPublisher {

        private final ApplicationEventPublisher eventPublisher;

        TransactionalPublisher(ApplicationEventPublisher eventPublisher) {
            this.eventPublisher = eventPublisher;
        }

        @Transactional
        public void publish(Object event) {
            eventPublisher.publishEvent(event);
        }
    }

    @BeforeEach
    void setUp() {
        cleanUp();
        User actor = userRepository.save(new User("Actor", "a-" + System.nanoTime() + "@b.test", "h", null));
        Workspace workspace = workspaceRepository.save(new Workspace("WS", "ws-" + System.nanoTime(), actor));
        workspaceMemberRepository.save(new WorkspaceMember(workspace, actor, WorkspaceRole.OWNER));
        Project project = projectRepository.save(
                new Project(workspace, "BugLens", "BL", null, ProjectStatus.ACTIVE));
        var component = componentRepository.save(
                new com.buglens.component.entity.Component(project, "Auth", null, ComponentStatus.ACTIVE));
        Release release = releaseRepository.save(
                new Release(project, "v2.4", null, ReleaseStatus.ACTIVE, LocalDate.of(2026, 9, 30)));

        actorId = actor.getId();
        componentId = component.getId();
        releaseId = release.getId();
    }

    @AfterEach
    void tearDown() {
        cleanUp();
    }

    private void cleanUp() {
        issueImpactRepository.deleteAll();
        componentHealthRepository.deleteAll();
        releaseRiskRepository.deleteAll();
        dependencyRepository.deleteAll();
        issueRepository.deleteAll();
        componentRepository.deleteAll();
        releaseRepository.deleteAll();
        projectRepository.deleteAll();
        workspaceMemberRepository.deleteAll();
        workspaceRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void creatingAnIssueScoresItInTheBackground() {
        Long issueId = createIssue(IssueSeverity.CRITICAL, IssuePriority.HIGH).getId();

        await(() -> impactOf(issueId) == 60, "impact score for a newly created issue");
        assertEquals(60, impactOf(issueId));
    }

    @Test
    void manuallyPublishedStatusChangeUpdatesImpactAndComponentHealth() {
        Issue issue = createIssue(IssueSeverity.CRITICAL, IssuePriority.HIGH);
        await(() -> impactOf(issue.getId()) == 60, "initial impact");

        issue.transitionTo(IssueStatus.RESOLVED);
        issueRepository.saveAndFlush(issue);

        transactionalPublisher.publish(IssueStatusChangedEvent.of(
                issue.getId(), actorId, IssueStatus.OPEN, IssueStatus.RESOLVED));

        await(() -> impactOf(issue.getId()) == 0, "impact reset to 0 after resolution");
        await(() -> healthOf(componentId) == 100, "component health restored once nothing is open");

        assertEquals(0, impactOf(issue.getId()));
        assertEquals(100, healthOf(componentId));
    }

    /**
     * Only a status change recalculates health and risk, so no such row exists until the first
     * transition. Creating a critical issue therefore leaves component health untouched — see
     * the note on creation-time staleness in the phase report.
     */
    @Test
    void creationDoesNotYetTouchHealthOrRisk() {
        Issue issue = createIssueInRelease(IssueSeverity.CRITICAL, IssuePriority.HIGH);

        await(() -> impactOf(issue.getId()) == 60, "impact is scored on creation");
        assertEquals(-1, healthOf(componentId), "no component health row until a status change");
        assertEquals(-1, riskOf(releaseId), "no release risk row until a status change");
    }

    @Test
    void transitionThroughTheWorkflowRecalculatesHealthAndRisk() {
        Issue issue = createIssueInRelease(IssueSeverity.CRITICAL, IssuePriority.HIGH);

        // IN_PROGRESS still counts as open: one critical issue costs 20 health and 30 risk.
        workflowService.transition(
                issue.getId(), new TransitionIssueRequest(IssueStatus.IN_PROGRESS, null), actorId);
        await(() -> healthOf(componentId) == 80, "health while one critical issue is open");
        await(() -> riskOf(releaseId) == 30, "risk while one critical issue is open");

        workflowService.transition(
                issue.getId(), new TransitionIssueRequest(IssueStatus.IN_REVIEW, null), actorId);
        workflowService.transition(
                issue.getId(), new TransitionIssueRequest(IssueStatus.RESOLVED, null), actorId);

        await(() -> healthOf(componentId) == 100, "health after the issue is resolved");
        await(() -> riskOf(releaseId) == 0, "risk after the issue is resolved");
        await(() -> impactOf(issue.getId()) == 0, "impact after the issue is resolved");
    }

    @Test
    void addingADependencyRescoresTheBlockingIssue() {
        Issue blocker = createIssue(IssueSeverity.LOW, IssuePriority.LOW);
        Issue blocked = createIssue(IssueSeverity.LOW, IssuePriority.LOW);

        await(() -> impactOf(blocker.getId()) == 15, "blocker impact before any dependency");

        dependencyService.addDependency(blocker.getId(), blocked.getId(), actorId);

        // 10 (LOW severity) + 5 (LOW priority) + 1 downstream * 5 = 20
        await(() -> impactOf(blocker.getId()) == 20, "blocker impact after gaining a dependency");
    }

    @Test
    void removingADependencyRescoresTheBlockingIssue() {
        Issue blocker = createIssue(IssueSeverity.LOW, IssuePriority.LOW);
        Issue blocked = createIssue(IssueSeverity.LOW, IssuePriority.LOW);
        dependencyService.addDependency(blocker.getId(), blocked.getId(), actorId);
        await(() -> impactOf(blocker.getId()) == 20, "blocker impact with a dependency");

        dependencyService.removeDependency(blocker.getId(), blocked.getId(), actorId);

        await(() -> impactOf(blocker.getId()) == 15, "blocker impact once the dependency is gone");
    }

    // ---------- helpers ----------

    private Issue createIssue(IssueSeverity severity, IssuePriority priority) {
        var response = issueService.create(
                new CreateIssueRequest("Issue", null, priority, severity, componentId, null, null),
                actorId
        );
        return issueRepository.findById(response.id()).orElseThrow();
    }

    private Issue createIssueInRelease(IssueSeverity severity, IssuePriority priority) {
        var response = issueService.create(
                new CreateIssueRequest("Issue", null, priority, severity, componentId, releaseId, null),
                actorId
        );
        return issueRepository.findById(response.id()).orElseThrow();
    }

    private int impactOf(Long issueId) {
        return issueImpactRepository.findById(issueId).map(i -> i.getImpactScore()).orElse(-1);
    }

    private int healthOf(Long componentId) {
        return componentHealthRepository.findById(componentId).map(h -> h.getHealthScore()).orElse(-1);
    }

    private int riskOf(Long releaseId) {
        return releaseRiskRepository.findById(releaseId).map(r -> r.getRiskScore()).orElse(-1);
    }

    /** Polls until the condition holds or the deadline passes, so async work is not raced. */
    private void await(BooleanSupplier condition, String what) {
        long deadline = System.currentTimeMillis() + TIMEOUT_MILLIS;
        while (System.currentTimeMillis() < deadline) {
            if (condition.getAsBoolean()) {
                return;
            }
            try {
                Thread.sleep(POLL_MILLIS);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                fail("interrupted while waiting for " + what);
            }
        }
        fail("Timed out after " + TIMEOUT_MILLIS + "ms waiting for " + what);
    }
}
