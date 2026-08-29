package com.buglens.comment.exception;

public class CommentIssueNotFoundException extends RuntimeException {

    public CommentIssueNotFoundException(Long issueId) {
        super("Issue not found: " + issueId);
    }
}
