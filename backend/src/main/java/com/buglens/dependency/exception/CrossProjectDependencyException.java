package com.buglens.dependency.exception;

public class CrossProjectDependencyException extends RuntimeException {

    public CrossProjectDependencyException(Long blockingIssueId, Long blockedIssueId) {
        super("Issues " + blockingIssueId + " and " + blockedIssueId + " belong to different projects");
    }
}
