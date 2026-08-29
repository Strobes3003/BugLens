package com.buglens.dependency.exception;

public class DependencyProjectNotFoundException extends RuntimeException {

    public DependencyProjectNotFoundException(Long projectId) {
        super("Project not found: " + projectId);
    }
}
