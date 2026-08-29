package com.buglens.intelligence.exception;

public class IntelligenceIssueNotFoundException extends RuntimeException {

    public IntelligenceIssueNotFoundException(Long issueId) {
        super("Issue not found: " + issueId);
    }
}
