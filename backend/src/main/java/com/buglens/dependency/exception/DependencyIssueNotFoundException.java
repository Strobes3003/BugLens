package com.buglens.dependency.exception;

public class DependencyIssueNotFoundException extends RuntimeException {

    public DependencyIssueNotFoundException(Long issueId) {
        super("Issue not found: " + issueId);
    }
}
