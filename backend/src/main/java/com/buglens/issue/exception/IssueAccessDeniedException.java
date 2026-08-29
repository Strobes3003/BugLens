package com.buglens.issue.exception;

public class IssueAccessDeniedException extends RuntimeException {

    public IssueAccessDeniedException() {
        super("You do not have access to this issue");
    }
}
