package com.buglens.dependency.service;

import com.buglens.component.entity.Component;
import com.buglens.dependency.dto.response.DependencyResponse;
import com.buglens.dependency.dto.response.IssueDependenciesResponse;
import com.buglens.dependency.entity.IssueDependency;
import com.buglens.dependency.exception.CrossProjectDependencyException;
import com.buglens.dependency.exception.CycleDetectedException;
import com.buglens.dependency.exception.DependencyAccessDeniedException;
import com.buglens.dependency.exception.DependencyIssueNotFoundException;
import com.buglens.dependency.exception.DependencyNotFoundException;
import com.buglens.dependency.exception.DuplicateDependencyException;
import com.buglens.dependency.exception.SelfDependencyException;
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
import org.springframework.context.ApplicationEventPublisher;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Graph behaviour is exercised against an in-memory adjacency map standing in for the edge
 * table, so the traversal itself is what is under test rather than Mockito stubbing.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DependencyServiceTest {

    private static final Long ACTOR_ID = 7L;
    private static final Long WORKSPACE_ID = 3L;
    private static final Long PROJECT_ID = 1L;

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
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private Workspace workspace;

    @Mock
    private Project project;

    @Mock
    private Component component;

    private DependencyService dependencyService;

    /** blocking id -> blocked ids. */
    private final Map<Long, List<Long>> graph = new LinkedHashMap<>();
    private final Map<Long, Issue> issues = new HashMap<>();

    @BeforeEach
    void setUp() {
        dependencyService = new DependencyService(
                dependencyRepository, issueRepository, workspaceAccessService, eventPublisher
        );

        lenient().when(workspace.getId()).thenReturn(WORKSPACE_ID);
        lenient().when(project.getId()).thenReturn(PROJECT_ID);
        lenient().when(project.getWorkspace()).thenReturn(workspace);
        lenient().when(component.getProject()).thenReturn(project);

        for (Long id : List.of(A, B, C, D, E)) {
            issues.put(id, issueWithId(id));
        }
        lenient().when(issueRepository.findById(any()))
                .thenAnswer(call -> Optional.ofNullable(issues.get(call.getArgument(0))));

        // Stands in for the recursive CTE: same contract, computed over the in-memory graph.
        lenient().when(dependencyRepository.findPathBetween(any(), any()))
                .thenAnswer(call -> pathBetween(call.getArgument(0), call.getArgument(1)));

        lenient().when(dependencyRepository.lockProjectGraph(any())).thenReturn(Boolean.TRUE);

        lenient().when(dependencyRepository.existsByBlockingIdAndBlockedId(any(), any()))
                .thenAnswer(call -> graph.getOrDefault(call.getArgument(0), List.of())
                        .contains(call.<Long>getArgument(1)));

        lenient().when(dependencyRepository.save(any(IssueDependency.class)))
                .thenAnswer(call -> {
                    IssueDependency edge = call.getArgument(0);
                    addEdge(edge.getBlocking().getId(), edge.getBlocked().getId());
                    return edge;
                });
    }

    private void addEdge(Long blocking, Long blocked) {
        graph.computeIfAbsent(blocking, key -> new ArrayList<>()).add(blocked);
    }

    /** Breadth-first walk mirroring what the CTE returns: the path from start to target. */
    private java.util.Optional<String> pathBetween(Long startId, Long targetId) {
        Map<Long, Long> cameFrom = new HashMap<>();
        java.util.Set<Long> seen = new java.util.HashSet<>();
        java.util.Deque<Long> queue = new java.util.ArrayDeque<>();

        seen.add(startId);
        queue.add(startId);

        while (!queue.isEmpty()) {
            Long current = queue.poll();
            if (current.equals(targetId)) {
                List<Long> path = new ArrayList<>();
                for (Long cursor = targetId; cursor != null; cursor = cameFrom.get(cursor)) {
                    path.add(cursor);
                    if (cursor.equals(startId)) {
                        break;
                    }
                }
                java.util.Collections.reverse(path);
                return java.util.Optional.of(
                        path.stream().map(String::valueOf).collect(java.util.stream.Collectors.joining(","))
                );
            }
            for (Long next : graph.getOrDefault(current, List.of())) {
                if (seen.add(next)) {
                    cameFrom.put(next, current);
                    queue.add(next);
                }
            }
        }
        return java.util.Optional.empty();
    }

    // ---------- cycle detection ----------

    @Test
    void takesProjectGraphLockBeforeReadingTheGraph() {
        add(A, B);

        org.mockito.InOrder order = org.mockito.Mockito.inOrder(dependencyRepository);
        order.verify(dependencyRepository).lockProjectGraph(PROJECT_ID);
        order.verify(dependencyRepository).findPathBetween(any(), any());
        order.verify(dependencyRepository).save(any(IssueDependency.class));
    }

    @Test
    void addsSimpleChainThenRejectsClosingEdge() {
        assertEquals(B, add(A, B).blockedIssue().id());
        assertEquals(C, add(B, C).blockedIssue().id());

        CycleDetectedException exception =
                assertThrows(CycleDetectedException.class, () -> add(C, A));

        assertEquals(List.of(A, B, C), exception.getCyclePath());
        assertEquals(List.of(B), graph.get(A));
    }

    @Test
    void rejectsDeepCycleAcrossFiveNodes() {
        add(A, B);
        add(B, C);
        add(C, D);
        add(D, E);

        CycleDetectedException exception =
                assertThrows(CycleDetectedException.class, () -> add(E, A));

        assertEquals(List.of(A, B, C, D, E), exception.getCyclePath());
        // Only the four legitimate edges were persisted; the closing edge was never saved.
        verify(dependencyRepository, org.mockito.Mockito.times(4)).save(any(IssueDependency.class));
        assertEquals(List.of(A, B, C, D), new ArrayList<>(graph.keySet()));
        assertTrue(graph.getOrDefault(E, List.of()).isEmpty());
    }

    @Test
    void rejectsDirectTwoNodeCycle() {
        add(A, B);

        assertThrows(CycleDetectedException.class, () -> add(B, A));
    }

    @Test
    void rejectsSelfDependency() {
        assertThrows(SelfDependencyException.class, () -> add(A, A));
        verify(dependencyRepository, never()).save(any());
    }

    @Test
    void allowsDiamondWhichIsNotACycle() {
        add(A, B);
        add(A, C);
        add(B, D);

        // D already reachable from A via B; adding C -> D keeps the graph acyclic.
        assertEquals(D, add(C, D).blockedIssue().id());
    }

    @Test
    void allowsEdgeBetweenUnrelatedBranches() {
        add(A, B);
        add(C, D);

        assertEquals(C, add(B, C).blockedIssue().id());
    }

    @Test
    void traversalTerminatesOnSharedDownstreamNodes() {
        add(A, C);
        add(B, C);
        add(C, D);

        // Reaching D from two directions must not loop or revisit.
        assertEquals(E, add(D, E).blockedIssue().id());
    }

    @Test
    void rejectsTransitiveCycleThroughBranch() {
        add(A, B);
        add(B, C);
        add(B, D);
        add(D, E);

        assertThrows(CycleDetectedException.class, () -> add(E, A));
    }

    // ---------- validation ----------

    @Test
    void rejectsDuplicateDependency() {
        add(A, B);

        assertThrows(DuplicateDependencyException.class, () -> add(A, B));
    }

    @Test
    void rejectsDependencyAcrossProjects() {
        Project otherProject = org.mockito.Mockito.mock(Project.class);
        Component otherComponent = org.mockito.Mockito.mock(Component.class);
        when(otherProject.getId()).thenReturn(99L);
        when(otherComponent.getProject()).thenReturn(otherProject);

        Issue foreign = new Issue(
                "OT-1", "Foreign", null, IssueStatus.OPEN,
                IssuePriority.HIGH, IssueSeverity.HIGH,
                otherComponent, null, null, null
        );
        issues.put(99L, foreign);

        assertThrows(CrossProjectDependencyException.class, () -> add(A, 99L));
        verify(dependencyRepository, never()).save(any());
    }

    @Test
    void rejectsMissingIssue() {
        assertThrows(DependencyIssueNotFoundException.class, () -> add(A, 404L));
    }

    @Test
    void rejectsNonMember() {
        doThrow(new WorkspaceAccessDeniedException())
                .when(workspaceAccessService).requireMember(WORKSPACE_ID, ACTOR_ID);

        assertThrows(DependencyAccessDeniedException.class, () -> add(A, B));
    }

    // ---------- removal and queries ----------

    @Test
    void addPublishesDependencyAddedEvent() {
        add(A, B);

        org.mockito.ArgumentCaptor<com.buglens.event.domain.DependencyAddedEvent> captor =
                org.mockito.ArgumentCaptor.forClass(com.buglens.event.domain.DependencyAddedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertEquals(A, captor.getValue().blockerId());
        assertEquals(B, captor.getValue().blockedId());
        assertEquals(ACTOR_ID, captor.getValue().actorId());
    }

    @Test
    void rejectedAddPublishesNoEvent() {
        add(A, B);
        add(B, C);
        org.mockito.Mockito.clearInvocations(eventPublisher);

        assertThrows(CycleDetectedException.class, () -> add(C, A));

        verify(eventPublisher, never()).publishEvent(any(Object.class));
    }

    @Test
    void removePublishesDependencyRemovedEvent() {
        IssueDependency edge = new IssueDependency(issues.get(A), issues.get(B));
        when(dependencyRepository.findByBlockingIdAndBlockedId(A, B)).thenReturn(Optional.of(edge));

        dependencyService.removeDependency(A, B, ACTOR_ID);

        org.mockito.ArgumentCaptor<com.buglens.event.domain.DependencyRemovedEvent> captor =
                org.mockito.ArgumentCaptor.forClass(com.buglens.event.domain.DependencyRemovedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertEquals(A, captor.getValue().blockerId());
        assertEquals(B, captor.getValue().blockedId());
    }

    @Test
    void removesExistingDependency() {
        IssueDependency edge = new IssueDependency(issues.get(A), issues.get(B));
        when(dependencyRepository.findByBlockingIdAndBlockedId(A, B)).thenReturn(Optional.of(edge));

        dependencyService.removeDependency(A, B, ACTOR_ID);

        verify(dependencyRepository).delete(edge);
    }

    @Test
    void removeRejectsMissingDependency() {
        when(dependencyRepository.findByBlockingIdAndBlockedId(A, B)).thenReturn(Optional.empty());

        assertThrows(
                DependencyNotFoundException.class,
                () -> dependencyService.removeDependency(A, B, ACTOR_ID)
        );
    }

    @Test
    void getDependenciesReturnsBothDirections() {
        when(dependencyRepository.findAllByBlockedIdOrderByCreatedAtAsc(B))
                .thenReturn(List.of(new IssueDependency(issues.get(A), issues.get(B))));
        when(dependencyRepository.findAllByBlockingIdOrderByCreatedAtAsc(B))
                .thenReturn(List.of(new IssueDependency(issues.get(B), issues.get(C))));

        IssueDependenciesResponse response = dependencyService.getDependencies(B, ACTOR_ID);

        assertEquals(List.of(A), response.blockedBy().stream().map(s -> s.id()).toList());
        assertEquals(List.of(C), response.blocking().stream().map(s -> s.id()).toList());
        assertTrue(response.issueKey().startsWith("BL-"));
    }

    private DependencyResponse add(Long blocking, Long blocked) {
        return dependencyService.addDependency(blocking, blocked, ACTOR_ID);
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
