package com.buglens.issue.exception;

public class IssueNotFoundException extends RuntimeException {

    public IssueNotFoundException(Long issueId) {
        super("Issue not found: " + issueId);
    }
}
