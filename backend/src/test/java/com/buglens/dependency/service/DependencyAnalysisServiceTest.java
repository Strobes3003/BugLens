package com.buglens.dependency.service;

import com.buglens.component.entity.Component;
import com.buglens.dependency.dto.response.DependencyAnalysisResponse;
import com.buglens.dependency.entity.IssueDependency;
import com.buglens.dependency.exception.DependencyAccessDeniedException;
import com.buglens.dependency.exception.DependencyIssueNotFoundException;
import com.buglens.dependency.repository.IssueDependencyRepository;
import com.buglens.issue.entity.Issue;
import com.buglens.issue.entity.IssuePriority;
import com.buglens.issue.entity.IssueSeverity;
import com.buglens.issue.entity.IssueStatus;
import com.buglens.issue.repository.IssueRepository;
import com.buglens.project.entity.Project;
import com.buglens.workspace.entity.Workspace;
import com.buglens.workspace.exception.WorkspaceAccessDeniedException;
import com.buglens.workspace.service.WorkspaceAccessService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;

/**
 * Exercises the aggregation the service performs on top of the traversal. The stubs mirror the
 * CTE contract (a de-duplicated node set); the CTEs themselves are proven against real
 * PostgreSQL in {@code IssueDependencyRepositoryTest}.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DependencyAnalysisServiceTest {

    private static final Long ACTOR_ID = 7L;
    private static final Long WORKSPACE_ID = 3L;

    private static final Long A = 1L;
    private static final Long B = 2L;
    private static final Long C = 3L;
    private static final Long D = 4L;
    private static final Long E = 5L;

    @Mock
    private IssueDependencyRepository dependencyRepository;

    @Mock
    private IssueRepository issueRepository;

    @Mock
    private WorkspaceAccessService workspaceAccessService;

    @Mock
    private Workspace workspace;

    @Mock
    private Project project;

    @Mock
    private Component component;

    private DependencyAnalysisService analysisService;

    private final Map<Long, List<Long>> graph = new LinkedHashMap<>();
    private final Map<Long, Issue> issues = new HashMap<>();

    @BeforeEach
    void setUp() {
        analysisService = new DependencyAnalysisService(
                dependencyRepository, issueRepository, workspaceAccessService
        );

        lenient().when(workspace.getId()).thenReturn(WORKSPACE_ID);
        lenient().when(project.getWorkspace()).thenReturn(workspace);
        lenient().when(component.getProject()).thenReturn(project);

        for (Long id : List.of(A, B, C, D, E)) {
            issues.put(id, issueWithId(id));
        }
        lenient().when(issueRepository.findById(any()))
                .thenAnswer(call -> Optional.ofNullable(issues.get(call.getArgument(0))));

        lenient().when(dependencyRepository.findAllByBlockingIdOrderByCreatedAtAsc(any()))
                .thenAnswer(call -> edgesFrom(call.getArgument(0)));
        lenient().when(dependencyRepository.findAllByBlockedIdOrderByCreatedAtAsc(any()))
                .thenAnswer(call -> edgesTo(call.getArgument(0)));

        lenient().when(dependencyRepository.findDeepDownstreamBlockedIds(any()))
                .thenAnswer(call -> reachable(call.getArgument(0), true));
        lenient().when(dependencyRepository.findDeepUpstreamBlockerIds(any()))
                .thenAnswer(call -> reachable(call.getArgument(0), false));
    }

    @Test
    void blastRadiusCountsWholeDownstreamChainAndBranch() {
        // A -> B -> C and A -> D
        edge(A, B);
        edge(B, C);
        edge(A, D);

        assertEquals(3, analyze(A).blastRadius());
    }

    @Test
    void blastRadiusCountsDiamondNodeOnlyOnce() {
        // A -> B, A -> C, B -> D, C -> D. D is reachable twice but is one affected issue.
        edge(A, B);
        edge(A, C);
        edge(B, D);
        edge(C, D);

        DependencyAnalysisResponse analysis = analyze(A);

        assertEquals(3, analysis.blastRadius());
        assertEquals(List.of(B, C), analysis.directBlocked().stream().map(s -> s.id()).toList());
    }

    @Test
    void blastRadiusIsZeroForLeafIssue() {
        edge(A, B);

        assertEquals(0, analyze(B).blastRadius());
    }

    @Test
    void blastRadiusExcludesTheIssueItself() {
        edge(A, B);
        edge(B, C);

        assertFalse(analyze(A).directBlocked().stream().anyMatch(s -> s.id().equals(A)));
        assertEquals(2, analyze(A).blastRadius());
    }

    @Test
    void totalBlockersCountsWholeUpstreamChain() {
        edge(A, B);
        edge(B, C);
        edge(D, C);

        DependencyAnalysisResponse analysis = analyze(C);

        assertEquals(3, analysis.totalBlockers());
        assertEquals(List.of(B, D), analysis.directBlockers().stream().map(s -> s.id()).toList());
    }

    @Test
    void bottleneckIsFalseBelowThreshold() {
        edge(A, B);
        edge(A, C);

        assertFalse(analyze(A).hasBottleneck());
    }

    @Test
    void bottleneckIsTrueAtThreshold() {
        edge(A, B);
        edge(A, C);
        edge(A, D);

        assertTrue(analyze(A).hasBottleneck());
        assertEquals(DependencyAnalysisService.BOTTLENECK_THRESHOLD, analyze(A).directBlocked().size());
    }

    @Test
    void bottleneckIsTrueAboveThreshold() {
        edge(A, B);
        edge(A, C);
        edge(A, D);
        edge(A, E);

        assertTrue(analyze(A).hasBottleneck());
    }

    @Test
    void longChainIsNotABottleneckDespiteLargeBlastRadius() {
        // A -> B -> C -> D -> E: blast radius 4, but only one issue waits on A directly.
        edge(A, B);
        edge(B, C);
        edge(C, D);
        edge(D, E);

        DependencyAnalysisResponse analysis = analyze(A);

        assertEquals(4, analysis.blastRadius());
        assertFalse(analysis.hasBottleneck());
    }

    @Test
    void isolatedIssueReportsEmptyAnalysis() {
        DependencyAnalysisResponse analysis = analyze(A);

        assertEquals(0, analysis.blastRadius());
        assertEquals(0, analysis.totalBlockers());
        assertTrue(analysis.directBlockers().isEmpty());
        assertTrue(analysis.directBlocked().isEmpty());
        assertFalse(analysis.hasBottleneck());
    }

    @Test
    void rejectsMissingIssue() {
        assertThrows(
                DependencyIssueNotFoundException.class,
                () -> analysisService.analyze(404L, ACTOR_ID)
        );
    }

    @Test
    void rejectsNonMember() {
        doThrow(new WorkspaceAccessDeniedException())
                .when(workspaceAccessService).requireMember(WORKSPACE_ID, ACTOR_ID);

        assertThrows(DependencyAccessDeniedException.class, () -> analyze(A));
    }

    private DependencyAnalysisResponse analyze(Long issueId) {
        return analysisService.analyze(issueId, ACTOR_ID);
    }

    private void edge(Long blocking, Long blocked) {
        graph.computeIfAbsent(blocking, key -> new ArrayList<>()).add(blocked);
    }

    private List<IssueDependency> edgesFrom(Long issueId) {
        return graph.getOrDefault(issueId, List.of()).stream()
                .map(blocked -> new IssueDependency(issues.get(issueId), issues.get(blocked)))
                .toList();
    }

    private List<IssueDependency> edgesTo(Long issueId) {
        List<IssueDependency> edges = new ArrayList<>();
        graph.forEach((blocking, blockedList) -> {
            if (blockedList.contains(issueId)) {
                edges.add(new IssueDependency(issues.get(blocking), issues.get(issueId)));
            }
        });
        return edges;
    }

    /** Mirrors the CTE: the set of nodes reachable in one direction, start node excluded. */
    private List<Long> reachable(Long startId, boolean downstream) {
        Set<Long> found = new LinkedHashSet<>();
        Deque<Long> queue = new ArrayDeque<>();
        queue.add(startId);

        while (!queue.isEmpty()) {
            Long current = queue.poll();
            for (Long next : neighbours(current, downstream)) {
                if (found.add(next)) {
                    queue.add(next);
                }
            }
        }
        found.remove(startId);
        return List.copyOf(found);
    }

    private List<Long> neighbours(Long node, boolean downstream) {
        if (downstream) {
            return graph.getOrDefault(node, List.of());
        }
        List<Long> blockers = new ArrayList<>();
        graph.forEach((blocking, blockedList) -> {
            if (blockedList.contains(node)) {
                blockers.add(blocking);
            }
        });
        return blockers;
    }

    private Issue issueWithId(Long id) {
        Issue issue = new Issue(
                "BL-" + id, "Issue " + id, null, IssueStatus.OPEN,
                IssuePriority.HIGH, IssueSeverity.HIGH,
                component, null, null, null
        );
        try {
            var field = Issue.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(issue, id);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
        return issue;
    }
}
