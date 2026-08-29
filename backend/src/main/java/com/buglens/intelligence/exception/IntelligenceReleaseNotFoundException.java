package com.buglens.intelligence.exception;

public class IntelligenceReleaseNotFoundException extends RuntimeException {

    public IntelligenceReleaseNotFoundException(Long releaseId) {
        super("Release not found: " + releaseId);
    }
}
