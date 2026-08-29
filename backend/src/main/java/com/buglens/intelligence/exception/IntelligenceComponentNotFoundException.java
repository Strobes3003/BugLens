package com.buglens.intelligence.exception;

public class IntelligenceComponentNotFoundException extends RuntimeException {

    public IntelligenceComponentNotFoundException(Long componentId) {
        super("Component not found: " + componentId);
    }
}
