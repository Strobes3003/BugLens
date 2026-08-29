package com.buglens.dependency.exception;

public class DependencyNotFoundException extends RuntimeException {

    public DependencyNotFoundException(Long blockingIssueId, Long blockedIssueId) {
        super("No dependency from issue " + blockingIssueId + " to issue " + blockedIssueId);
    }
}
