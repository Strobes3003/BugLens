package com.buglens.comment.exception;

public class InvalidCommentBodyException extends RuntimeException {

    public InvalidCommentBodyException() {
        super("Comment body must not be blank");
    }
}
