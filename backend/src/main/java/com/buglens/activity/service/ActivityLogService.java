package com.buglens.activity.service;

import com.buglens.activity.dto.response.ActivityLogResponse;
import com.buglens.activity.entity.ActivityAction;
import com.buglens.activity.entity.ActivityLog;
import com.buglens.activity.exception.ActivityAccessDeniedException;
import com.buglens.activity.exception.ActivityIssueNotFoundException;
import com.buglens.activity.repository.ActivityLogRepository;
import com.buglens.auth.entity.User;
import com.buglens.auth.repository.UserRepository;
import com.buglens.event.domain.IssueCommentedEvent;
import com.buglens.event.domain.IssueCreatedEvent;
import com.buglens.event.domain.IssueDomainEvent;
import com.buglens.event.domain.IssueStatusChangedEvent;
import com.buglens.event.domain.IssueUpdatedEvent;
import com.buglens.issue.entity.Issue;
import com.buglens.issue.repository.IssueRepository;
import com.buglens.project.entity.Project;
import com.buglens.workspace.exception.WorkspaceAccessDeniedException;
import com.buglens.workspace.service.WorkspaceAccessService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;
import java.util.Optional;

/**
 * Turns domain events into issue history.
 *
 * <p>This is the only component that knows how an event reads to a human. The publishing
 * services depend on {@code ApplicationEventPublisher} alone and have no reference to this class.
 *
 * <p>Listeners run at {@link TransactionPhase#AFTER_COMMIT}, so nothing is recorded for a change
 * that was rolled back. Because the publishing transaction has already completed by then, each
 * handler opens its own transaction with {@link Propagation#REQUIRES_NEW} — without it the save
 * would join a transaction that can never commit and the row would be silently discarded.
 */
@Service
@Transactional(readOnly = true)
public class ActivityLogService {

    private static final Logger log = LoggerFactory.getLogger(ActivityLogService.class);

    private final ActivityLogRepository activityLogRepository;
    private final IssueRepository issueRepository;
    private final UserRepository userRepository;
    private final WorkspaceAccessService workspaceAccessService;

    public ActivityLogService(
            ActivityLogRepository activityLogRepository,
            IssueRepository issueRepository,
            UserRepository userRepository,
            WorkspaceAccessService workspaceAccessService
    ) {
        this.activityLogRepository = activityLogRepository;
        this.issueRepository = issueRepository;
        this.userRepository = userRepository;
        this.workspaceAccessService = workspaceAccessService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onIssueCreated(IssueCreatedEvent event) {
        record(event, ActivityAction.ISSUE_CREATED, "created issue " + event.issueKey());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onIssueUpdated(IssueUpdatedEvent event) {
        record(event, ActivityAction.ISSUE_UPDATED, describeUpdate(event.changedFields()));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onIssueStatusChanged(IssueStatusChangedEvent event) {
        record(
                event,
                ActivityAction.STATUS_CHANGED,
                "changed status from " + event.oldStatus() + " to " + event.newStatus()
        );
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onIssueCommented(IssueCommentedEvent event) {
        record(event, ActivityAction.COMMENT_ADDED, "added a comment");
    }

    public List<ActivityLogResponse> listForIssue(Long issueId, Long actorId) {
        Issue issue = issueRepository.findById(issueId)
                .orElseThrow(() -> new ActivityIssueNotFoundException(issueId));
        requireMember(issue.getComponent().getProject(), actorId);

        return activityLogRepository.findAllByIssueIdOrderByCreatedAtAsc(issueId)
                .stream()
                .map(ActivityLogResponse::from)
                .toList();
    }

    /**
     * Persists one history row. The issue or actor can legitimately be gone by the time this
     * runs — the change committed, then something else removed the row — so a missing reference
     * is logged and skipped rather than thrown. Throwing here would not roll anything back; the
     * originating transaction has already committed.
     */
    private void record(IssueDomainEvent event, ActivityAction action, String description) {
        Optional<Issue> issue = issueRepository.findById(event.issueId());
        Optional<User> actor = userRepository.findById(event.actorId());

        if (issue.isEmpty() || actor.isEmpty()) {
            log.warn(
                    "Skipping {} activity for issue {} by actor {}: referenced entity no longer exists",
                    action, event.issueId(), event.actorId()
            );
            return;
        }

        activityLogRepository.save(new ActivityLog(issue.get(), actor.get(), action, description));
    }

    private String describeUpdate(List<String> changedFields) {
        if (changedFields.isEmpty()) {
            return "updated the issue";
        }
        return "updated " + String.join(", ", changedFields);
    }

    private void requireMember(Project project, Long actorId) {
        try {
            workspaceAccessService.requireMember(project.getWorkspace().getId(), actorId);
        } catch (WorkspaceAccessDeniedException exception) {
            throw new ActivityAccessDeniedException();
        }
    }
}
