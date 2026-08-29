package com.buglens.comment.dto.response;

import com.buglens.auth.entity.User;
import com.buglens.comment.entity.Comment;

import java.time.Instant;

public record CommentResponse(
        Long id,
        Long issueId,
        CommentAuthor author,
        String body,
        boolean isEdited,
        Instant createdAt,
        Instant updatedAt
) {

    public record CommentAuthor(Long id, String name, String email) {

        public static CommentAuthor from(User user) {
            return new CommentAuthor(user.getId(), user.getName(), user.getEmail());
        }
    }

    public static CommentResponse from(Comment comment) {
        return new CommentResponse(
                comment.getId(),
                comment.getIssue().getId(),
                CommentAuthor.from(comment.getAuthor()),
                comment.getBody(),
                comment.isEdited(),
                comment.getCreatedAt(),
                comment.getUpdatedAt()
        );
    }
}
