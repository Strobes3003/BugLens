package com.buglens.comment.exception;

public class CommentAccessDeniedException extends RuntimeException {

    public CommentAccessDeniedException(String detail) {
        super(detail);
    }
}
