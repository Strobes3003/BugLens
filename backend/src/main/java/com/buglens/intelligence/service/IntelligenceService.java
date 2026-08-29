package com.buglens.intelligence.service;

import com.buglens.component.entity.Component;
import com.buglens.component.repository.ComponentRepository;
import com.buglens.dependency.repository.IssueDependencyRepository;
import com.buglens.intelligence.entity.ComponentHealth;
import com.buglens.intelligence.entity.IssueImpact;
import com.buglens.intelligence.entity.ReleaseRisk;
import com.buglens.intelligence.exception.IntelligenceComponentNotFoundException;
import com.buglens.intelligence.exception.IntelligenceIssueNotFoundException;
import com.buglens.intelligence.exception.IntelligenceProjectNotFoundException;
import com.buglens.intelligence.exception.IntelligenceReleaseNotFoundException;
import com.buglens.intelligence.repository.ComponentHealthRepository;
import com.buglens.intelligence.repository.IssueImpactRepository;
import com.buglens.intelligence.repository.ReleaseRiskRepository;
import com.buglens.issue.entity.Issue;
import com.buglens.issue.entity.IssuePriority;
import com.buglens.issue.entity.IssueSeverity;
import com.buglens.issue.entity.IssueStatus;
import com.buglens.issue.repository.IssueRepository;
import com.buglens.project.repository.ProjectRepository;
import com.buglens.release.entity.Release;
import com.buglens.release.repository.ReleaseRepository;
import com.buglens.intelligence.exception.IntelligenceAccessDeniedException;
import com.buglens.project.entity.Project;
import com.buglens.workspace.exception.WorkspaceAccessDeniedException;
import com.buglens.workspace.service.WorkspaceAccessService;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

/**
 * Calculates and stores the intelligence scores.
 *
 * <p>Scores are persisted rather than derived on read, so the frontend can rank and display them
 * without paying for a graph traversal on every request.
 *
 * <p>On severity weights: the architecture's formula names four tiers Blocker/Critical/Major/
 * Minor, but {@code IssueSeverity} is LOW/MEDIUM/HIGH/CRITICAL. The weights are therefore mapped
 * by rank — highest tier 40, then 30, 20, 10 — preserving the intended shape of the formula.
 */
@Service
@Transactional(readOnly = true)
public class IntelligenceService {

    /** Statuses that mean an issue is still costing the team something. */
    private static final Set<IssueStatus> OPEN_STATUSES =
            Set.of(IssueStatus.OPEN, IssueStatus.IN_PROGRESS, IssueStatus.IN_REVIEW);

    private static final int IMPACT_MAX = 100;
    private static final int BLAST_RADIUS_WEIGHT = 5;
    private static final int BLAST_RADIUS_CAP = 30;

    private static final int HEALTH_MAX = 100;
    private static final int HEALTH_CRITICAL_PENALTY = 20;
    private static final int HEALTH_HIGH_PENALTY = 10;
    private static final int HEALTH_MEDIUM_PENALTY = 5;

    private static final int RISK_MAX = 100;
    private static final int RISK_TOP_SEVERITY_WEIGHT = 30;
    private static final int RISK_SECOND_SEVERITY_WEIGHT = 15;
    private static final int RISK_BOTTLENECK_WEIGHT = 10;
    private static final int BOTTLENECK_THRESHOLD = 3;

    private final IssueRepository issueRepository;
    private final ComponentRepository componentRepository;
    private final ReleaseRepository releaseRepository;
    private final ProjectRepository projectRepository;
    private final IssueDependencyRepository dependencyRepository;
    private final IssueImpactRepository issueImpactRepository;
    private final ComponentHealthRepository componentHealthRepository;
    private final ReleaseRiskRepository releaseRiskRepository;
    private final WorkspaceAccessService workspaceAccessService;

    public IntelligenceService(
            IssueRepository issueRepository,
            ComponentRepository componentRepository,
            ReleaseRepository releaseRepository,
            ProjectRepository projectRepository,
            IssueDependencyRepository dependencyRepository,
            IssueImpactRepository issueImpactRepository,
            ComponentHealthRepository componentHealthRepository,
            ReleaseRiskRepository releaseRiskRepository,
            WorkspaceAccessService workspaceAccessService
    ) {
        this.issueRepository = issueRepository;
        this.componentRepository = componentRepository;
        this.releaseRepository = releaseRepository;
        this.projectRepository = projectRepository;
        this.dependencyRepository = dependencyRepository;
        this.issueImpactRepository = issueImpactRepository;
        this.componentHealthRepository = componentHealthRepository;
        this.releaseRiskRepository = releaseRiskRepository;
        this.workspaceAccessService = workspaceAccessService;
    }

    // ---------- Impact Score ----------

    /**
     * severity + priority + min(blastRadius * 5, 30), capped at 100.
     *
     * <p>A resolved or closed issue scores 0 outright: it is no longer costing anything, however
     * severe it was or however much still sits downstream of it.
     */
    @Transactional
    public IssueImpact calculateAndSaveIssueImpact(Long issueId) {
        Issue issue = issueRepository.findById(issueId)
                .orElseThrow(() -> new IntelligenceIssueNotFoundException(issueId));

        int score = impactScoreFor(issue);

        IssueImpact impact = issueImpactRepository.findById(issueId).orElse(null);
        if (impact == null) {
            return issueImpactRepository.save(new IssueImpact(issue, score));
        }
        impact.recalculatedAs(score);
        return impact;
    }

    int impactScoreFor(Issue issue) {
        if (!isOpen(issue.getStatus())) {
            return 0;
        }

        int blastRadius = dependencyRepository.findDeepDownstreamBlockedIds(issue.getId()).size();
        int blastContribution = Math.min(blastRadius * BLAST_RADIUS_WEIGHT, BLAST_RADIUS_CAP);

        int raw = severityWeight(issue.getSeverity())
                + priorityWeight(issue.getPriority())
                + blastContribution;

        return Math.min(raw, IMPACT_MAX);
    }

    /** Blocker/Critical/Major/Minor in the architecture, mapped onto this codebase's tiers. */
    private int severityWeight(IssueSeverity severity) {
        return switch (severity) {
            case CRITICAL -> 40;
            case HIGH -> 30;
            case MEDIUM -> 20;
            case LOW -> 10;
        };
    }

    private int priorityWeight(IssuePriority priority) {
        return switch (priority) {
            case CRITICAL -> 30;
            case HIGH -> 20;
            case MEDIUM -> 10;
            case LOW -> 5;
        };
    }

    // ---------- Component Health ----------

    /** 100 - (critical * 20 + high * 10 + medium * 5), floored at 0. */
    @Transactional
    public ComponentHealth calculateAndSaveComponentHealth(Long componentId) {
        Component component = componentRepository.findById(componentId)
                .orElseThrow(() -> new IntelligenceComponentNotFoundException(componentId));

        long critical = openInComponent(componentId, IssueSeverity.CRITICAL);
        long high = openInComponent(componentId, IssueSeverity.HIGH);
        long medium = openInComponent(componentId, IssueSeverity.MEDIUM);

        long penalty = critical * HEALTH_CRITICAL_PENALTY
                + high * HEALTH_HIGH_PENALTY
                + medium * HEALTH_MEDIUM_PENALTY;

        int score = (int) Math.max(0, HEALTH_MAX - penalty);

        ComponentHealth health = componentHealthRepository.findById(componentId).orElse(null);
        if (health == null) {
            return componentHealthRepository.save(new ComponentHealth(component, score));
        }
        health.recalculatedAs(score);
        return health;
    }

    // ---------- Release Risk ----------

    /** (topSeverity * 30 + secondSeverity * 15 + bottlenecks * 10), capped at 100. */
    @Transactional
    public ReleaseRisk calculateAndSaveReleaseRisk(Long releaseId) {
        Release release = releaseRepository.findById(releaseId)
                .orElseThrow(() -> new IntelligenceReleaseNotFoundException(releaseId));

        long critical = openInRelease(releaseId, IssueSeverity.CRITICAL);
        long high = openInRelease(releaseId, IssueSeverity.HIGH);
        long bottlenecks = dependencyRepository.countBottlenecksInRelease(releaseId, BOTTLENECK_THRESHOLD);

        long raw = critical * RISK_TOP_SEVERITY_WEIGHT
                + high * RISK_SECOND_SEVERITY_WEIGHT
                + bottlenecks * RISK_BOTTLENECK_WEIGHT;

        int score = (int) Math.min(raw, RISK_MAX);

        ReleaseRisk risk = releaseRiskRepository.findById(releaseId).orElse(null);
        if (risk == null) {
            return releaseRiskRepository.save(new ReleaseRisk(release, score));
        }
        risk.recalculatedAs(score);
        return risk;
    }

    // ---------- Fix Next ----------

    /**
     * The team's next N issues, highest impact first. Ties break on age so the ordering is stable
     * between calls and the issue that has waited longest wins.
     */
    public List<IssueImpact> getFixNext(Long projectId, int limit) {
        if (!projectRepository.existsById(projectId)) {
            throw new IntelligenceProjectNotFoundException(projectId);
        }
        return issueImpactRepository.findFixNext(projectId, PageRequest.of(0, Math.max(1, limit)));
    }

    // ---------- access-checked reads ----------

    /**
     * Reads the stored score. If nothing has been calculated yet the score is computed once and
     * persisted, so a never-scored entity returns a real number rather than a 404.
     *
     * <p>This fallback exists because nothing recalculates scores automatically yet; once the
     * event-driven recalculation lands, reads will almost always hit a stored row.
     */
    @Transactional
    public ComponentHealth getComponentHealth(Long componentId, Long actorId) {
        Component component = componentRepository.findById(componentId)
                .orElseThrow(() -> new IntelligenceComponentNotFoundException(componentId));
        requireMember(component.getProject(), actorId);

        return componentHealthRepository.findById(componentId)
                .orElseGet(() -> calculateAndSaveComponentHealth(componentId));
    }

    @Transactional
    public ReleaseRisk getReleaseRisk(Long releaseId, Long actorId) {
        Release release = releaseRepository.findById(releaseId)
                .orElseThrow(() -> new IntelligenceReleaseNotFoundException(releaseId));
        requireMember(release.getProject(), actorId);

        return releaseRiskRepository.findById(releaseId)
                .orElseGet(() -> calculateAndSaveReleaseRisk(releaseId));
    }

    public List<IssueImpact> getFixNext(Long projectId, int limit, Long actorId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new IntelligenceProjectNotFoundException(projectId));
        requireMember(project, actorId);

        return issueImpactRepository.findFixNext(projectId, PageRequest.of(0, Math.max(1, limit)));
    }

    private void requireMember(Project project, Long actorId) {
        try {
            workspaceAccessService.requireMember(project.getWorkspace().getId(), actorId);
        } catch (WorkspaceAccessDeniedException exception) {
            throw new IntelligenceAccessDeniedException();
        }
    }

    private long openInComponent(Long componentId, IssueSeverity severity) {
        return issueRepository.countByComponentIdAndSeverityAndStatusIn(componentId, severity, OPEN_STATUSES);
    }

    private long openInRelease(Long releaseId, IssueSeverity severity) {
        return issueRepository.countByReleaseIdAndSeverityAndStatusIn(releaseId, severity, OPEN_STATUSES);
    }

    private boolean isOpen(IssueStatus status) {
        return OPEN_STATUSES.contains(status);
    }
}
