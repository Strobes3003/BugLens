package com.buglens.dependency.exception;

public class DuplicateDependencyException extends RuntimeException {

    public DuplicateDependencyException(Long blockingIssueId, Long blockedIssueId) {
        super("Issue " + blockingIssueId + " already blocks issue " + blockedIssueId);
    }
}
