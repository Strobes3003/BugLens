package com.buglens.component.exception;

public class ComponentProjectNotFoundException extends RuntimeException {

    public ComponentProjectNotFoundException(Long projectId) {
        super("Project not found: " + projectId);
    }
}
