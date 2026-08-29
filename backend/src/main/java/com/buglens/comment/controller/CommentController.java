package com.buglens.comment.controller;

import com.buglens.comment.dto.request.UpdateCommentRequest;
import com.buglens.comment.dto.response.CommentResponse;
import com.buglens.comment.service.CommentService;
import com.buglens.common.security.UserPrincipal;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/comments")
public class CommentController {

    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @PatchMapping("/{commentId}")
    public CommentResponse update(
            @PathVariable Long commentId,
            @Valid @RequestBody UpdateCommentRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return commentService.updateComment(commentId, request, principal.getId());
    }

    @DeleteMapping("/{commentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @PathVariable Long commentId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        commentService.deleteComment(commentId, principal.getId());
    }
}
