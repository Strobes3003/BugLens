package com.buglens.issue.exception;

public class IssueProjectNotFoundException extends RuntimeException {

    public IssueProjectNotFoundException(Long projectId) {
        super("Project not found: " + projectId);
    }
}
