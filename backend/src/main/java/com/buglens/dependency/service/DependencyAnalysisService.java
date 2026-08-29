package com.buglens.dependency.service;

import com.buglens.dependency.dto.response.DependencyAnalysisResponse;
import com.buglens.dependency.dto.response.DependencyIssueSummary;
import com.buglens.dependency.exception.DependencyAccessDeniedException;
import com.buglens.dependency.exception.DependencyIssueNotFoundException;
import com.buglens.dependency.repository.IssueDependencyRepository;
import com.buglens.issue.entity.Issue;
import com.buglens.issue.repository.IssueRepository;
import com.buglens.project.entity.Project;
import com.buglens.workspace.exception.WorkspaceAccessDeniedException;
import com.buglens.workspace.service.WorkspaceAccessService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Answers the graph questions the intelligence layer will build on: what blocks this issue, what
 * does it block, and how much of the project is downstream of it.
 */
@Service
@Transactional(readOnly = true)
public class DependencyAnalysisService {

    /**
     * An issue directly blocking this many others is treated as a chokepoint. Counts direct
     * outgoing edges rather than blast radius: a long thin chain has a large blast radius but
     * only one thing waiting on it, which is a different shape of problem from many teams
     * blocked on one fix.
     */
    static final int BOTTLENECK_THRESHOLD = 3;

    private final IssueDependencyRepository dependencyRepository;
    private final IssueRepository issueRepository;
    private final WorkspaceAccessService workspaceAccessService;

    public DependencyAnalysisService(
            IssueDependencyRepository dependencyRepository,
            IssueRepository issueRepository,
            WorkspaceAccessService workspaceAccessService
    ) {
        this.dependencyRepository = dependencyRepository;
        this.issueRepository = issueRepository;
        this.workspaceAccessService = workspaceAccessService;
    }

    public DependencyAnalysisResponse analyze(Long issueId, Long actorId) {
        Issue issue = issueRepository.findById(issueId)
                .orElseThrow(() -> new DependencyIssueNotFoundException(issueId));
        requireMember(issue.getComponent().getProject(), actorId);

        List<DependencyIssueSummary> directBlockers =
                dependencyRepository.findAllByBlockedIdOrderByCreatedAtAsc(issueId)
                        .stream()
                        .map(edge -> DependencyIssueSummary.from(edge.getBlocking()))
                        .toList();

        List<DependencyIssueSummary> directBlocked =
                dependencyRepository.findAllByBlockingIdOrderByCreatedAtAsc(issueId)
                        .stream()
                        .map(edge -> DependencyIssueSummary.from(edge.getBlocked()))
                        .toList();

        // Both traversals are one query each; the database returns a de-duplicated node set.
        int blastRadius = dependencyRepository.findDeepDownstreamBlockedIds(issueId).size();
        int totalBlockers = dependencyRepository.findDeepUpstreamBlockerIds(issueId).size();

        return new DependencyAnalysisResponse(
                issue.getId(),
                issue.getIssueKey(),
                blastRadius,
                totalBlockers,
                directBlockers,
                directBlocked,
                directBlocked.size() >= BOTTLENECK_THRESHOLD
        );
    }

    private void requireMember(Project project, Long actorId) {
        try {
            workspaceAccessService.requireMember(project.getWorkspace().getId(), actorId);
        } catch (WorkspaceAccessDeniedException exception) {
            throw new DependencyAccessDeniedException();
        }
    }
}
