package com.buglens.workflow.service;

import com.buglens.comment.dto.request.CreateCommentRequest;
import com.buglens.comment.service.CommentService;
import com.buglens.event.domain.IssueStatusChangedEvent;
import com.buglens.issue.dto.response.IssueResponse;
import com.buglens.issue.entity.Issue;
import com.buglens.issue.entity.IssueStatus;
import com.buglens.issue.exception.IssueAccessDeniedException;
import com.buglens.issue.exception.IssueNotFoundException;
import com.buglens.issue.repository.IssueRepository;
import com.buglens.project.entity.Project;
import com.buglens.workflow.domain.IssueWorkflow;
import com.buglens.workflow.dto.request.TransitionIssueRequest;
import com.buglens.workflow.dto.response.AllowedTransitionsResponse;
import com.buglens.workspace.exception.WorkspaceAccessDeniedException;
import com.buglens.workspace.service.WorkspaceAccessService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class WorkflowService {

    private final IssueRepository issueRepository;
    private final WorkspaceAccessService workspaceAccessService;
    private final CommentService commentService;
    private final ApplicationEventPublisher eventPublisher;

    public WorkflowService(
            IssueRepository issueRepository,
            WorkspaceAccessService workspaceAccessService,
            CommentService commentService,
            ApplicationEventPublisher eventPublisher
    ) {
        this.issueRepository = issueRepository;
        this.workspaceAccessService = workspaceAccessService;
        this.commentService = commentService;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public IssueResponse transition(Long issueId, TransitionIssueRequest request, Long actorId) {
        Issue issue = getIssue(issueId);
        requireMember(issue.getComponent().getProject(), actorId);

        IssueStatus target = request.targetStatus();
        IssueStatus previous = issue.getStatus();
        IssueWorkflow.requireAllowed(previous, target);
        issue.transitionTo(target);

        persistTransitionComment(issueId, request.comment(), actorId);

        eventPublisher.publishEvent(
                IssueStatusChangedEvent.of(issue.getId(), actorId, previous, target)
        );

        return IssueResponse.from(issue);
    }

    public AllowedTransitionsResponse getAllowedTransitions(Long issueId, Long actorId) {
        Issue issue = getIssue(issueId);
        requireMember(issue.getComponent().getProject(), actorId);

        return new AllowedTransitionsResponse(
                issue.getStatus(),
                IssueWorkflow.allowedFrom(issue.getStatus())
        );
    }

    /**
     * A transition may carry an optional note. It is stored as an ordinary comment authored by
     * the actor, in the same transaction as the status change, so a rolled-back transition leaves
     * no orphaned note behind.
     */
    private void persistTransitionComment(Long issueId, String comment, Long actorId) {
        if (comment == null || comment.isBlank()) {
            return;
        }
        commentService.addComment(issueId, new CreateCommentRequest(comment), actorId);
    }

    private Issue getIssue(Long issueId) {
        return issueRepository.findById(issueId)
                .orElseThrow(() -> new IssueNotFoundException(issueId));
    }

    private void requireMember(Project project, Long actorId) {
        try {
            workspaceAccessService.requireMember(project.getWorkspace().getId(), actorId);
        } catch (WorkspaceAccessDeniedException exception) {
            throw new IssueAccessDeniedException();
        }
    }
}
