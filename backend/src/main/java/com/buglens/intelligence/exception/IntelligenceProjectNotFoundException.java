package com.buglens.intelligence.exception;

public class IntelligenceProjectNotFoundException extends RuntimeException {

    public IntelligenceProjectNotFoundException(Long projectId) {
        super("Project not found: " + projectId);
    }
}
