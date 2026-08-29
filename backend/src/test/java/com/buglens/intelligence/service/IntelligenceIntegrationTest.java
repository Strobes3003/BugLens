package com.buglens.intelligence.service;

import com.buglens.auth.entity.User;
import com.buglens.auth.repository.UserRepository;
import com.buglens.component.entity.Component;
import com.buglens.component.entity.ComponentStatus;
import com.buglens.component.repository.ComponentRepository;
import com.buglens.dependency.entity.IssueDependency;
import com.buglens.dependency.repository.IssueDependencyRepository;
import com.buglens.intelligence.entity.IssueImpact;
import com.buglens.intelligence.repository.ComponentHealthRepository;
import com.buglens.intelligence.repository.IssueImpactRepository;
import com.buglens.intelligence.repository.ReleaseRiskRepository;
import com.buglens.issue.entity.Issue;
import com.buglens.issue.entity.IssuePriority;
import com.buglens.issue.entity.IssueSeverity;
import com.buglens.issue.entity.IssueStatus;
import com.buglens.issue.repository.IssueRepository;
import com.buglens.project.entity.Project;
import com.buglens.project.entity.ProjectStatus;
import com.buglens.project.repository.ProjectRepository;
import com.buglens.release.entity.Release;
import com.buglens.release.entity.ReleaseStatus;
import com.buglens.release.repository.ReleaseRepository;
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

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Exercises the scoring pipeline against a real database: real counting queries, the real
 * bottleneck GROUP BY, the real blast-radius CTE, and the real Fix Next ordering.
 */
@SpringBootTest
class IntelligenceIntegrationTest {

    @Autowired private IntelligenceService intelligenceService;
    @Autowired private IssueDependencyRepository dependencyRepository;
    @Autowired private IssueImpactRepository issueImpactRepository;
    @Autowired private ComponentHealthRepository componentHealthRepository;
    @Autowired private ReleaseRiskRepository releaseRiskRepository;
    @Autowired private IssueRepository issueRepository;
    @Autowired private ComponentRepository componentRepository;
    @Autowired private ReleaseRepository releaseRepository;
    @Autowired private ProjectRepository projectRepository;
    @Autowired private WorkspaceMemberRepository workspaceMemberRepository;
    @Autowired private WorkspaceRepository workspaceRepository;
    @Autowired private UserRepository userRepository;

    private Long projectId;
    private Long componentId;
    private Long releaseId;
    private User actor;
    private Component component;
    private Release release;

    @BeforeEach
    void setUp() {
        cleanUp();
        actor = userRepository.save(new User("Actor", "a-" + System.nanoTime() + "@b.test", "h", null));
        Workspace workspace = workspaceRepository.save(
                new Workspace("WS", "ws-" + System.nanoTime(), actor));
        workspaceMemberRepository.save(new WorkspaceMember(workspace, actor, WorkspaceRole.OWNER));
        Project project = projectRepository.save(
                new Project(workspace, "BugLens", "BL", null, ProjectStatus.ACTIVE));
        component = componentRepository.save(
                new Component(project, "Auth", null, ComponentStatus.ACTIVE));
        release = releaseRepository.save(
                new Release(project, "v2.4", null, ReleaseStatus.ACTIVE, LocalDate.of(2026, 9, 30)));

        projectId = project.getId();
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
    void impactScoreUsesRealBlastRadiusFromTheGraph() {
        Issue root = issue("BL-1", IssueSeverity.CRITICAL, IssuePriority.HIGH, IssueStatus.OPEN);
        Issue down1 = issue("BL-2", IssueSeverity.LOW, IssuePriority.LOW, IssueStatus.OPEN);
        Issue down2 = issue("BL-3", IssueSeverity.LOW, IssuePriority.LOW, IssueStatus.OPEN);
        dependencyRepository.save(new IssueDependency(root, down1));
        dependencyRepository.save(new IssueDependency(down1, down2));

        // 40 (CRITICAL) + 20 (HIGH) + 2 downstream * 5 = 70
        assertEquals(70, intelligenceService.calculateAndSaveIssueImpact(root.getId()).getImpactScore());
    }

    @Test
    void componentHealthCountsOnlyOpenIssues() {
        issue("BL-1", IssueSeverity.CRITICAL, IssuePriority.HIGH, IssueStatus.OPEN);
        issue("BL-2", IssueSeverity.HIGH, IssuePriority.HIGH, IssueStatus.IN_REVIEW);
        issue("BL-3", IssueSeverity.CRITICAL, IssuePriority.HIGH, IssueStatus.CLOSED);
        issue("BL-4", IssueSeverity.CRITICAL, IssuePriority.HIGH, IssueStatus.RESOLVED);

        // Only the OPEN critical and IN_REVIEW high count: 100 - (20 + 10) = 70
        assertEquals(70,
                intelligenceService.calculateAndSaveComponentHealth(componentId).getHealthScore());
    }

    @Test
    void releaseRiskCountsBottlenecksFromTheRealGraph() {
        Issue chokepoint = releaseIssue("BL-1", IssueSeverity.LOW, IssueStatus.OPEN);
        for (int i = 2; i <= 4; i++) {
            dependencyRepository.save(new IssueDependency(
                    chokepoint, releaseIssue("BL-" + i, IssueSeverity.LOW, IssueStatus.OPEN)));
        }

        // No critical/high severity in the release; one bottleneck with 3 outgoing edges = 10
        assertEquals(10, intelligenceService.calculateAndSaveReleaseRisk(releaseId).getRiskScore());
    }

    @Test
    void resolvedBottleneckStopsCountingTowardsRisk() {
        Issue chokepoint = releaseIssue("BL-1", IssueSeverity.LOW, IssueStatus.OPEN);
        for (int i = 2; i <= 4; i++) {
            dependencyRepository.save(new IssueDependency(
                    chokepoint, releaseIssue("BL-" + i, IssueSeverity.LOW, IssueStatus.OPEN)));
        }
        assertEquals(10, intelligenceService.calculateAndSaveReleaseRisk(releaseId).getRiskScore());

        chokepoint.transitionTo(IssueStatus.RESOLVED);
        issueRepository.saveAndFlush(chokepoint);

        assertEquals(0, intelligenceService.calculateAndSaveReleaseRisk(releaseId).getRiskScore());
    }

    @Test
    void fixNextRanksByImpactThenAgeAndRespectsLimit() {
        Issue low = issue("BL-1", IssueSeverity.LOW, IssuePriority.LOW, IssueStatus.OPEN);
        Issue highA = issue("BL-2", IssueSeverity.CRITICAL, IssuePriority.CRITICAL, IssueStatus.OPEN);
        Issue highB = issue("BL-3", IssueSeverity.CRITICAL, IssuePriority.CRITICAL, IssueStatus.OPEN);

        intelligenceService.calculateAndSaveIssueImpact(low.getId());
        intelligenceService.calculateAndSaveIssueImpact(highA.getId());
        intelligenceService.calculateAndSaveIssueImpact(highB.getId());

        List<IssueImpact> ranked = intelligenceService.getFixNext(projectId, 10, actor.getId());

        assertEquals(3, ranked.size());
        assertEquals(70, ranked.get(0).getImpactScore());
        assertEquals(70, ranked.get(1).getImpactScore());
        assertEquals(15, ranked.get(2).getImpactScore());
        // Equal scores break on age: BL-2 was created before BL-3.
        assertEquals("BL-2", ranked.get(0).getIssue().getIssueKey());
        assertEquals("BL-3", ranked.get(1).getIssue().getIssueKey());

        assertEquals(2, intelligenceService.getFixNext(projectId, 2, actor.getId()).size());
    }

    @Test
    void recalculationOverwritesTheStoredScoreInPlace() {
        Issue issue = issue("BL-1", IssueSeverity.CRITICAL, IssuePriority.CRITICAL, IssueStatus.OPEN);

        assertEquals(70, intelligenceService.calculateAndSaveIssueImpact(issue.getId()).getImpactScore());

        issue.transitionTo(IssueStatus.RESOLVED);
        issueRepository.saveAndFlush(issue);

        assertEquals(0, intelligenceService.calculateAndSaveIssueImpact(issue.getId()).getImpactScore());
        assertEquals(1, issueImpactRepository.count(), "recalculation must update, not insert a second row");
    }

    private Issue issue(String key, IssueSeverity severity, IssuePriority priority, IssueStatus status) {
        return issueRepository.saveAndFlush(new Issue(
                key, "Issue " + key, null, status, priority, severity, component, null, actor, null));
    }

    private Issue releaseIssue(String key, IssueSeverity severity, IssueStatus status) {
        return issueRepository.saveAndFlush(new Issue(
                key, "Issue " + key, null, status, IssuePriority.LOW, severity,
                component, release, actor, null));
    }
}
