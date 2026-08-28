package com.buglens.project.exception;

public class ProjectWorkspaceNotFoundException extends RuntimeException {

    public ProjectWorkspaceNotFoundException(Long workspaceId) {
        super("Workspace not found: " + workspaceId);
    }
}
