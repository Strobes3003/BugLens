package com.buglens.dependency.service;

import com.buglens.dependency.dto.response.DependencyIssueSummary;
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
import com.buglens.issue.repository.IssueRepository;
import com.buglens.project.entity.Project;
import com.buglens.workspace.exception.WorkspaceAccessDeniedException;
import com.buglens.workspace.service.WorkspaceAccessService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class DependencyService {

    private final IssueDependencyRepository dependencyRepository;
    private final IssueRepository issueRepository;
    private final WorkspaceAccessService workspaceAccessService;

    public DependencyService(
            IssueDependencyRepository dependencyRepository,
            IssueRepository issueRepository,
            WorkspaceAccessService workspaceAccessService
    ) {
        this.dependencyRepository = dependencyRepository;
        this.issueRepository = issueRepository;
        this.workspaceAccessService = workspaceAccessService;
    }

    @Transactional
    public DependencyResponse addDependency(Long blockingIssueId, Long blockedIssueId, Long actorId) {
        if (blockingIssueId.equals(blockedIssueId)) {
            throw new SelfDependencyException(blockingIssueId);
        }

        Issue blocking = requireIssue(blockingIssueId);
        Issue blocked = requireIssue(blockedIssueId);

        Project project = blocking.getComponent().getProject();
        requireMember(project, actorId);
        if (!project.getId().equals(blocked.getComponent().getProject().getId())) {
            throw new CrossProjectDependencyException(blockingIssueId, blockedIssueId);
        }

        // Everything below reads the graph and then writes to it. Serialise those two steps
        // against other writers in this project before the first read.
        dependencyRepository.lockProjectGraph(project.getId());

        if (dependencyRepository.existsByBlockingIdAndBlockedId(blockingIssueId, blockedIssueId)) {
            throw new DuplicateDependencyException(blockingIssueId, blockedIssueId);
        }

        requireNoCycle(blockingIssueId, blockedIssueId);

        return DependencyResponse.from(
                dependencyRepository.save(new IssueDependency(blocking, blocked))
        );
    }

    @Transactional
    public void removeDependency(Long blockingIssueId, Long blockedIssueId, Long actorId) {
        Issue blocking = requireIssue(blockingIssueId);
        requireMember(blocking.getComponent().getProject(), actorId);

        IssueDependency dependency = dependencyRepository
                .findByBlockingIdAndBlockedId(blockingIssueId, blockedIssueId)
                .orElseThrow(() -> new DependencyNotFoundException(blockingIssueId, blockedIssueId));

        dependencyRepository.delete(dependency);
    }

    public IssueDependenciesResponse getDependencies(Long issueId, Long actorId) {
        Issue issue = requireIssue(issueId);
        requireMember(issue.getComponent().getProject(), actorId);

        List<DependencyIssueSummary> blockedBy =
                dependencyRepository.findAllByBlockedIdOrderByCreatedAtAsc(issueId)
                        .stream()
                        .map(edge -> DependencyIssueSummary.from(edge.getBlocking()))
                        .toList();

        List<DependencyIssueSummary> blocking =
                dependencyRepository.findAllByBlockingIdOrderByCreatedAtAsc(issueId)
                        .stream()
                        .map(edge -> DependencyIssueSummary.from(edge.getBlocked()))
                        .toList();

        return new IssueDependenciesResponse(issue.getId(), issue.getIssueKey(), blockedBy, blocking);
    }

    /** Issues that block {@code issueId} (incoming edges). */
    public List<DependencyIssueSummary> getBlockedBy(Long issueId, Long actorId) {
        return getDependencies(issueId, actorId).blockedBy();
    }

    /** Issues that {@code issueId} blocks (outgoing edges). */
    public List<DependencyIssueSummary> getBlocking(Long issueId, Long actorId) {
        return getDependencies(issueId, actorId).blocking();
    }

    /**
     * Rejects an edge {@code blocking -> blocked} that would close a loop.
     *
     * <p>The graph is acyclic before this call, so the only way the new edge can create a cycle
     * is if {@code blocking} is already reachable downstream of {@code blocked}. The search
     * therefore starts at {@code blocked} and follows outgoing edges.
     *
     * <p>The traversal itself runs in PostgreSQL as a recursive CTE: one query for the whole
     * walk, rather than one per visited node. The query also returns the path it took, so the
     * caller is told which existing chain blocks the edge instead of just that one exists.
     *
     * @throws CycleDetectedException with the path blocked -> ... -> blocking
     */
    private void requireNoCycle(Long blockingIssueId, Long blockedIssueId) {
        dependencyRepository.findPathBetween(blockedIssueId, blockingIssueId)
                .ifPresent(path -> {
                    throw new CycleDetectedException(blockingIssueId, blockedIssueId, parsePath(path));
                });
    }

    /** Turns the CTE's comma-joined path into ids. */
    private List<Long> parsePath(String path) {
        if (path == null || path.isBlank()) {
            return List.of();
        }
        return Arrays.stream(path.split(","))
                .map(String::trim)
                .filter(part -> !part.isEmpty())
                .map(Long::valueOf)
                .toList();
    }

    private Issue requireIssue(Long issueId) {
        return issueRepository.findById(issueId)
                .orElseThrow(() -> new DependencyIssueNotFoundException(issueId));
    }

    private void requireMember(Project project, Long actorId) {
        try {
            workspaceAccessService.requireMember(project.getWorkspace().getId(), actorId);
        } catch (WorkspaceAccessDeniedException exception) {
            throw new DependencyAccessDeniedException();
        }
    }
}
