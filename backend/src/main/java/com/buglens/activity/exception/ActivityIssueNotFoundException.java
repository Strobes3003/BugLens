package com.buglens.activity.exception;

public class ActivityIssueNotFoundException extends RuntimeException {

    public ActivityIssueNotFoundException(Long issueId) {
        super("Issue not found: " + issueId);
    }
}
