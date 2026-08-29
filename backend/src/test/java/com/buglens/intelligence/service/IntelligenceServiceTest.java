package com.buglens.intelligence.service;

import com.buglens.component.entity.Component;
import com.buglens.component.repository.ComponentRepository;
import com.buglens.dependency.repository.IssueDependencyRepository;
import com.buglens.intelligence.entity.ComponentHealth;
import com.buglens.intelligence.entity.IssueImpact;
import com.buglens.intelligence.entity.ReleaseRisk;
import com.buglens.intelligence.exception.IntelligenceIssueNotFoundException;
import com.buglens.intelligence.repository.ComponentHealthRepository;
import com.buglens.intelligence.repository.IssueImpactRepository;
import com.buglens.intelligence.repository.ReleaseRiskRepository;
import com.buglens.issue.entity.Issue;
import com.buglens.issue.entity.IssuePriority;
import com.buglens.issue.entity.IssueSeverity;
import com.buglens.issue.entity.IssueStatus;
import com.buglens.issue.repository.IssueRepository;
import com.buglens.project.entity.Project;
import com.buglens.project.repository.ProjectRepository;
import com.buglens.release.entity.Release;
import com.buglens.release.repository.ReleaseRepository;
import com.buglens.workspace.entity.Workspace;
import com.buglens.workspace.service.WorkspaceAccessService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class IntelligenceServiceTest {

    private static final Long ISSUE_ID = 1L;
    private static final Long COMPONENT_ID = 10L;
    private static final Long RELEASE_ID = 20L;

    @Mock private IssueRepository issueRepository;
    @Mock private ComponentRepository componentRepository;
    @Mock private ReleaseRepository releaseRepository;
    @Mock private ProjectRepository projectRepository;
    @Mock private IssueDependencyRepository dependencyRepository;
    @Mock private IssueImpactRepository issueImpactRepository;
    @Mock private ComponentHealthRepository componentHealthRepository;
    @Mock private ReleaseRiskRepository releaseRiskRepository;
    @Mock private WorkspaceAccessService workspaceAccessService;

    @Mock private Workspace workspace;
    @Mock private Project project;
    @Mock private Component component;
    @Mock private Release release;

    private IntelligenceService intelligenceService;

    @BeforeEach
    void setUp() {
        intelligenceService = new IntelligenceService(
                issueRepository, componentRepository, releaseRepository, projectRepository,
                dependencyRepository, issueImpactRepository, componentHealthRepository,
                releaseRiskRepository, workspaceAccessService
        );

        lenient().when(workspace.getId()).thenReturn(3L);
        lenient().when(project.getWorkspace()).thenReturn(workspace);
        lenient().when(component.getProject()).thenReturn(project);
        lenient().when(component.getName()).thenReturn("Auth");
        lenient().when(release.getProject()).thenReturn(project);

        lenient().when(componentRepository.findById(COMPONENT_ID)).thenReturn(Optional.of(component));
        lenient().when(releaseRepository.findById(RELEASE_ID)).thenReturn(Optional.of(release));
        lenient().when(issueImpactRepository.findById(any())).thenReturn(Optional.empty());
        lenient().when(componentHealthRepository.findById(any())).thenReturn(Optional.empty());
        lenient().when(releaseRiskRepository.findById(any())).thenReturn(Optional.empty());
        lenient().when(issueImpactRepository.save(any())).thenAnswer(c -> c.getArgument(0));
        lenient().when(componentHealthRepository.save(any())).thenAnswer(c -> c.getArgument(0));
        lenient().when(releaseRiskRepository.save(any())).thenAnswer(c -> c.getArgument(0));
        lenient().when(dependencyRepository.countBottlenecksInRelease(anyLong(), anyInt())).thenReturn(0L);
    }

    // ---------- Impact Score ----------

    /** The worked example from the specification: 40 + 20 + (2 * 5) = 70. */
    @Test
    void impactScoreMatchesTheSpecifiedWorkedExample() {
        givenIssue(IssueSeverity.CRITICAL, IssuePriority.HIGH, IssueStatus.OPEN, 2);

        assertEquals(70, impact());
    }

    @ParameterizedTest(name = "severity {0} contributes {1}")
    @CsvSource({"CRITICAL,40", "HIGH,30", "MEDIUM,20", "LOW,10"})
    void severityWeightsByRank(IssueSeverity severity, int expected) {
        givenIssue(severity, IssuePriority.LOW, IssueStatus.OPEN, 0);

        assertEquals(expected + 5, impact());
    }

    @ParameterizedTest(name = "priority {0} contributes {1}")
    @CsvSource({"CRITICAL,30", "HIGH,20", "MEDIUM,10", "LOW,5"})
    void priorityWeights(IssuePriority priority, int expected) {
        givenIssue(IssueSeverity.LOW, priority, IssueStatus.OPEN, 0);

        assertEquals(10 + expected, impact());
    }

    @Test
    void blastRadiusContributionIsCappedAtThirty() {
        givenIssue(IssueSeverity.LOW, IssuePriority.LOW, IssueStatus.OPEN, 6);
        assertEquals(10 + 5 + 30, impact());

        givenIssue(IssueSeverity.LOW, IssuePriority.LOW, IssueStatus.OPEN, 50);
        assertEquals(10 + 5 + 30, impact(), "blast radius contribution must not exceed 30");
    }

    @Test
    void impactScoreIsCappedAtOneHundred() {
        givenIssue(IssueSeverity.CRITICAL, IssuePriority.CRITICAL, IssueStatus.OPEN, 100);

        assertEquals(100, impact());
    }

    @ParameterizedTest(name = "{0} issues score 0")
    @EnumSource(value = IssueStatus.class, names = {"RESOLVED", "CLOSED"})
    void resolvedAndClosedIssuesAlwaysScoreZero(IssueStatus status) {
        givenIssue(IssueSeverity.CRITICAL, IssuePriority.CRITICAL, status, 50);

        assertEquals(0, impact());
    }

    @ParameterizedTest(name = "{0} issues are scored normally")
    @EnumSource(value = IssueStatus.class, names = {"OPEN", "IN_PROGRESS", "IN_REVIEW"})
    void openStatusesAreScoredNormally(IssueStatus status) {
        givenIssue(IssueSeverity.CRITICAL, IssuePriority.CRITICAL, status, 0);

        assertEquals(70, impact());
    }

    @Test
    void impactRejectsMissingIssue() {
        when(issueRepository.findById(404L)).thenReturn(Optional.empty());

        assertThrows(
                IntelligenceIssueNotFoundException.class,
                () -> intelligenceService.calculateAndSaveIssueImpact(404L)
        );
    }

    // ---------- Component Health ----------

    @Test
    void componentHealthSubtractsWeightedOpenIssues() {
        givenOpenInComponent(1, 2, 3);

        // 100 - (1*20 + 2*10 + 3*5) = 45
        assertEquals(45, health());
    }

    @Test
    void componentHealthIsOneHundredWhenClean() {
        givenOpenInComponent(0, 0, 0);

        assertEquals(100, health());
    }

    @Test
    void componentHealthFloorsAtZeroAndNeverGoesNegative() {
        givenOpenInComponent(6, 0, 0);
        assertEquals(0, health(), "100 - 120 must floor at 0");

        givenOpenInComponent(50, 50, 50);
        assertEquals(0, health(), "health must never be negative");
    }

    @Test
    void componentHealthIsExactlyZeroAtTheBoundary() {
        givenOpenInComponent(5, 0, 0);

        assertEquals(0, health());
    }

    // ---------- Release Risk ----------

    @Test
    void releaseRiskSumsWeightedOpenIssuesAndBottlenecks() {
        givenOpenInRelease(1, 2);
        when(dependencyRepository.countBottlenecksInRelease(eq(RELEASE_ID), anyInt())).thenReturn(1L);

        // 1*30 + 2*15 + 1*10 = 70
        assertEquals(70, risk());
    }

    @Test
    void releaseRiskIsZeroWhenClean() {
        givenOpenInRelease(0, 0);

        assertEquals(0, risk());
    }

    @Test
    void releaseRiskCapsAtOneHundred() {
        givenOpenInRelease(10, 10);
        when(dependencyRepository.countBottlenecksInRelease(eq(RELEASE_ID), anyInt())).thenReturn(10L);

        // raw = 300 + 150 + 100 = 550
        assertEquals(100, risk(), "risk must cap at 100");
    }

    @Test
    void releaseRiskIsExactlyOneHundredAtTheBoundary() {
        givenOpenInRelease(0, 0);
        when(dependencyRepository.countBottlenecksInRelease(eq(RELEASE_ID), anyInt())).thenReturn(10L);

        assertEquals(100, risk());
    }

    @Test
    void releaseRiskCountsBottlenecksOnly() {
        givenOpenInRelease(0, 0);
        when(dependencyRepository.countBottlenecksInRelease(eq(RELEASE_ID), anyInt())).thenReturn(3L);

        assertEquals(30, risk());
    }

    // ---------- Fix Next ----------

    @Test
    void fixNextIsOrderedByImpactThenAge() {
        // The repository applies the ordering; this pins that the service preserves it and does
        // not re-sort or reverse the result on the way out.
        List<IssueImpact> ranked = List.of(
                impactOf(90, "BL-3"), impactOf(70, "BL-1"), impactOf(70, "BL-2"), impactOf(10, "BL-4")
        );
        when(projectRepository.existsById(5L)).thenReturn(true);
        when(issueImpactRepository.findFixNext(eq(5L), any())).thenReturn(ranked);

        List<Integer> scores = intelligenceService.getFixNext(5L, 10)
                .stream().map(IssueImpact::getImpactScore).toList();

        assertEquals(List.of(90, 70, 70, 10), scores);
        assertTrue(isDescending(scores));
    }

    @Test
    void fixNextRequestsTheAskedForPageSize() {
        when(projectRepository.existsById(5L)).thenReturn(true);
        when(issueImpactRepository.findFixNext(eq(5L), any())).thenReturn(List.of());

        intelligenceService.getFixNext(5L, 3);

        org.mockito.ArgumentCaptor<org.springframework.data.domain.Pageable> captor =
                org.mockito.ArgumentCaptor.forClass(org.springframework.data.domain.Pageable.class);
        org.mockito.Mockito.verify(issueImpactRepository).findFixNext(eq(5L), captor.capture());
        assertEquals(3, captor.getValue().getPageSize());
    }

    // ---------- helpers ----------

    private void givenIssue(IssueSeverity severity, IssuePriority priority, IssueStatus status, int blastRadius) {
        Issue issue = new Issue(
                "BL-1", "Issue", null, status, priority, severity, component, null, null, null
        );
        setId(issue, ISSUE_ID);
        when(issueRepository.findById(ISSUE_ID)).thenReturn(Optional.of(issue));
        when(dependencyRepository.findDeepDownstreamBlockedIds(ISSUE_ID))
                .thenReturn(Stream.iterate(100L, n -> n + 1).limit(blastRadius).collect(Collectors.toList()));
    }

    private void givenOpenInComponent(int critical, int high, int medium) {
        stubComponentCount(IssueSeverity.CRITICAL, critical);
        stubComponentCount(IssueSeverity.HIGH, high);
        stubComponentCount(IssueSeverity.MEDIUM, medium);
    }

    private void stubComponentCount(IssueSeverity severity, long count) {
        when(issueRepository.countByComponentIdAndSeverityAndStatusIn(
                eq(COMPONENT_ID), eq(severity), any(Collection.class))).thenReturn(count);
    }

    private void givenOpenInRelease(int critical, int high) {
        when(issueRepository.countByReleaseIdAndSeverityAndStatusIn(
                eq(RELEASE_ID), eq(IssueSeverity.CRITICAL), any(Collection.class))).thenReturn((long) critical);
        when(issueRepository.countByReleaseIdAndSeverityAndStatusIn(
                eq(RELEASE_ID), eq(IssueSeverity.HIGH), any(Collection.class))).thenReturn((long) high);
    }

    private int impact() {
        return intelligenceService.calculateAndSaveIssueImpact(ISSUE_ID).getImpactScore();
    }

    private int health() {
        return intelligenceService.calculateAndSaveComponentHealth(COMPONENT_ID).getHealthScore();
    }

    private int risk() {
        return intelligenceService.calculateAndSaveReleaseRisk(RELEASE_ID).getRiskScore();
    }

    private IssueImpact impactOf(int score, String key) {
        Issue issue = new Issue(
                key, "Issue " + key, null, IssueStatus.OPEN,
                IssuePriority.HIGH, IssueSeverity.HIGH, component, null, null, null
        );
        return new IssueImpact(issue, score);
    }

    private boolean isDescending(List<Integer> scores) {
        for (int i = 1; i < scores.size(); i++) {
            if (scores.get(i) > scores.get(i - 1)) {
                return false;
            }
        }
        return true;
    }

    private void setId(Issue issue, Long id) {
        try {
            var field = Issue.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(issue, id);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
