package com.buglens.comment.exception;

public class CommentAuthorNotFoundException extends RuntimeException {

    public CommentAuthorNotFoundException(Long authorId) {
        super("Comment author not found: " + authorId);
    }
}
