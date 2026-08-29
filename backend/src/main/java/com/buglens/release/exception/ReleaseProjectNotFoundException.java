package com.buglens.release.exception;

public class ReleaseProjectNotFoundException extends RuntimeException {

    public ReleaseProjectNotFoundException(Long projectId) {
        super("Project not found: " + projectId);
    }
}
