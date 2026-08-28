package com.buglens.workspace.exception;

public class WorkspaceNotFoundException extends RuntimeException {

    public WorkspaceNotFoundException(Long workspaceId) {
        super("Workspace not found: " + workspaceId);
    }
}
