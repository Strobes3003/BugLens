package com.buglens.issue.exception;

public class IssueKeyNotFoundException extends RuntimeException {

    public IssueKeyNotFoundException(String issueKey) {
        super("Issue not found: " + issueKey);
    }
}
