package com.buglens.intelligence.listener;

import com.buglens.event.domain.DependencyAddedEvent;
import com.buglens.event.domain.DependencyDomainEvent;
import com.buglens.event.domain.DependencyRemovedEvent;
import com.buglens.event.domain.IssueCreatedEvent;
import com.buglens.event.domain.IssueStatusChangedEvent;
import com.buglens.event.domain.IssueUpdatedEvent;
import com.buglens.intelligence.service.IntelligenceService;
import com.buglens.issue.entity.Issue;
import com.buglens.issue.repository.IssueRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Optional;

/**
 * Keeps the stored intelligence scores in step with the domain.
 *
 * <p>Listeners run at {@link TransactionPhase#AFTER_COMMIT}, so a rolled-back change never
 * produces a score, and {@link Async}, so the scoring math — which walks the dependency graph —
 * happens on a background thread instead of on the request that triggered it.
 *
 * <p>{@code @Async} moves the work to another thread, which means the publishing transaction is
 * long gone by the time it runs. Each handler therefore needs its own transaction; the
 * {@code IntelligenceService} methods carry {@code @Transactional} for exactly that reason.
 *
 * <p>Every handler swallows its own failures. An exception thrown here cannot roll anything back
 * — the originating transaction has already committed — and on an async thread it would vanish
 * into the executor's uncaught handler. Logging it keeps a stale score visible rather than
 * silent.
 */
@Component
public class IntelligenceEventListener {

    private static final Logger log = LoggerFactory.getLogger(IntelligenceEventListener.class);

    private final IntelligenceService intelligenceService;
    private final IssueRepository issueRepository;

    public IntelligenceEventListener(
            IntelligenceService intelligenceService,
            IssueRepository issueRepository
    ) {
        this.intelligenceService = intelligenceService;
        this.issueRepository = issueRepository;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onIssueCreated(IssueCreatedEvent event) {
        recalculateImpact(event.issueId(), "issue created");
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onIssueUpdated(IssueUpdatedEvent event) {
        recalculateImpact(event.issueId(), "issue updated");
    }

    /**
     * A status change moves an issue in or out of the "open" set, so it shifts more than its own
     * score: its component's health and its release's risk both count open issues, and both go
     * stale the moment the status flips.
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onIssueStatusChanged(IssueStatusChangedEvent event) {
        recalculateImpact(event.issueId(), "status changed");

        Optional<Issue> issue = issueRepository.findById(event.issueId());
        if (issue.isEmpty()) {
            log.warn("Skipping health and risk recalculation: issue {} no longer exists", event.issueId());
            return;
        }

        Long componentId = issue.get().getComponent().getId();
        safely(
                () -> intelligenceService.calculateAndSaveComponentHealth(componentId),
                "component health for component " + componentId
        );

        if (issue.get().getRelease() != null) {
            Long releaseId = issue.get().getRelease().getId();
            safely(
                    () -> intelligenceService.calculateAndSaveReleaseRisk(releaseId),
                    "release risk for release " + releaseId
            );
        }
    }

    /**
     * Adding or removing an edge changes the blocker's blast radius, which is a term in its
     * impact score.
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onDependencyAdded(DependencyAddedEvent event) {
        recalculateBlockerImpact(event);
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onDependencyRemoved(DependencyRemovedEvent event) {
        recalculateBlockerImpact(event);
    }

    private void recalculateBlockerImpact(DependencyDomainEvent event) {
        recalculateImpact(event.blockerId(), "dependency changed");
    }

    private void recalculateImpact(Long issueId, String reason) {
        safely(
                () -> intelligenceService.calculateAndSaveIssueImpact(issueId),
                "impact for issue " + issueId + " (" + reason + ")"
        );
    }

    private void safely(Runnable recalculation, String what) {
        try {
            recalculation.run();
        } catch (RuntimeException exception) {
            log.warn("Intelligence recalculation failed for {}", what, exception);
        }
    }
}
