package com.buglens.intelligence.exception;

public class IntelligenceAccessDeniedException extends RuntimeException {

    public IntelligenceAccessDeniedException() {
        super("You do not have access to this project");
    }
}
