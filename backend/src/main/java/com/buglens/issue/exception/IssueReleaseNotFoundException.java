package com.buglens.issue.exception;

public class IssueReleaseNotFoundException extends RuntimeException {

    public IssueReleaseNotFoundException(Long releaseId) {
        super("Release not found: " + releaseId);
    }
}
