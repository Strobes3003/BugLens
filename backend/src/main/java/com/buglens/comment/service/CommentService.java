package com.buglens.comment.service;

import com.buglens.auth.entity.User;
import com.buglens.auth.repository.UserRepository;
import com.buglens.comment.dto.request.CreateCommentRequest;
import com.buglens.comment.dto.request.UpdateCommentRequest;
import com.buglens.comment.dto.response.CommentResponse;
import com.buglens.comment.entity.Comment;
import com.buglens.comment.exception.CommentAccessDeniedException;
import com.buglens.comment.exception.CommentAuthorNotFoundException;
import com.buglens.comment.exception.CommentIssueNotFoundException;
import com.buglens.comment.exception.CommentNotFoundException;
import com.buglens.comment.exception.InvalidCommentBodyException;
import com.buglens.comment.repository.CommentRepository;
import com.buglens.issue.entity.Issue;
import com.buglens.issue.repository.IssueRepository;
import com.buglens.project.entity.Project;
import com.buglens.workspace.exception.WorkspaceAccessDeniedException;
import com.buglens.workspace.service.WorkspaceAccessService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class CommentService {

    private final CommentRepository commentRepository;
    private final IssueRepository issueRepository;
    private final UserRepository userRepository;
    private final WorkspaceAccessService workspaceAccessService;

    public CommentService(
            CommentRepository commentRepository,
            IssueRepository issueRepository,
            UserRepository userRepository,
            WorkspaceAccessService workspaceAccessService
    ) {
        this.commentRepository = commentRepository;
        this.issueRepository = issueRepository;
        this.userRepository = userRepository;
        this.workspaceAccessService = workspaceAccessService;
    }

    @Transactional
    public CommentResponse addComment(Long issueId, CreateCommentRequest request, Long authorId) {
        Issue issue = requireIssue(issueId);
        requireMember(issue.getComponent().getProject(), authorId);

        User author = userRepository.findById(authorId)
                .orElseThrow(() -> new CommentAuthorNotFoundException(authorId));

        Comment comment = new Comment(issue, author, normalizeBody(request.body()));
        return CommentResponse.from(commentRepository.save(comment));
    }

    public List<CommentResponse> listForIssue(Long issueId, Long actorId) {
        Issue issue = requireIssue(issueId);
        requireMember(issue.getComponent().getProject(), actorId);

        return commentRepository.findAllByIssueIdOrderByCreatedAtAsc(issueId)
                .stream()
                .map(CommentResponse::from)
                .toList();
    }

    /**
     * Only the author may edit a comment. Workspace managers deliberately cannot — editing
     * someone else's words is a different power from removing them.
     */
    @Transactional
    public CommentResponse updateComment(Long commentId, UpdateCommentRequest request, Long requesterId) {
        Comment comment = requireComment(commentId);
        requireMember(comment.getIssue().getComponent().getProject(), requesterId);

        if (!isAuthor(comment, requesterId)) {
            throw new CommentAccessDeniedException("Only the comment author can edit this comment");
        }

        comment.editBody(normalizeBody(request.body()));
        return CommentResponse.from(comment);
    }

    /**
     * The author may delete their own comment; a workspace manager (OWNER/ADMIN) may delete any
     * comment for moderation. Manager status is resolved here rather than supplied by the caller,
     * so the authorization decision cannot be spoofed from the transport layer.
     */
    @Transactional
    public void deleteComment(Long commentId, Long requesterId) {
        Comment comment = requireComment(commentId);
        Project project = comment.getIssue().getComponent().getProject();
        requireMember(project, requesterId);

        if (!isAuthor(comment, requesterId) && !isManager(project, requesterId)) {
            throw new CommentAccessDeniedException(
                    "Only the comment author or a workspace manager can delete this comment"
            );
        }

        commentRepository.delete(comment);
    }

    private boolean isAuthor(Comment comment, Long requesterId) {
        return comment.getAuthor().getId().equals(requesterId);
    }

    private boolean isManager(Project project, Long actorId) {
        try {
            workspaceAccessService.requireMemberManager(project.getWorkspace().getId(), actorId);
            return true;
        } catch (WorkspaceAccessDeniedException exception) {
            return false;
        }
    }

    private Comment requireComment(Long commentId) {
        return commentRepository.findById(commentId)
                .orElseThrow(() -> new CommentNotFoundException(commentId));
    }

    private Issue requireIssue(Long issueId) {
        return issueRepository.findById(issueId)
                .orElseThrow(() -> new CommentIssueNotFoundException(issueId));
    }

    private void requireMember(Project project, Long actorId) {
        try {
            workspaceAccessService.requireMember(project.getWorkspace().getId(), actorId);
        } catch (WorkspaceAccessDeniedException exception) {
            throw new CommentAccessDeniedException("You do not have access to this issue");
        }
    }

    private String normalizeBody(String body) {
        String normalized = body == null ? null : body.trim();
        if (normalized == null || normalized.isBlank()) {
            throw new InvalidCommentBodyException();
        }
        return normalized;
    }
}
