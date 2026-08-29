package com.buglens.comment.controller;

import com.buglens.comment.dto.request.CreateCommentRequest;
import com.buglens.comment.dto.response.CommentResponse;
import com.buglens.comment.service.CommentService;
import com.buglens.common.security.UserPrincipal;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Issue-scoped comment endpoints. Separate from {@link CommentController} because the path is
 * nested under {@code /api/issues}, which cannot share the {@code /api/comments} class mapping.
 */
@RestController
@RequestMapping("/api/issues/{issueId}/comments")
public class IssueCommentController {

    private final CommentService commentService;

    public IssueCommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CommentResponse create(
            @PathVariable Long issueId,
            @Valid @RequestBody CreateCommentRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return commentService.addComment(issueId, request, principal.getId());
    }

    @GetMapping
    public List<CommentResponse> list(
            @PathVariable Long issueId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return commentService.listForIssue(issueId, principal.getId());
    }
}
