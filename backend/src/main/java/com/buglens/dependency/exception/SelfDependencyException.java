package com.buglens.dependency.exception;

public class SelfDependencyException extends RuntimeException {

    public SelfDependencyException(Long issueId) {
        super("Issue " + issueId + " cannot depend on itself");
    }
}
