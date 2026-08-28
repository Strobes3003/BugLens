package com.buglens.workspace.exception;

public class WorkspaceAccessDeniedException extends RuntimeException {

    public WorkspaceAccessDeniedException() {
        super("You do not have permission to perform this workspace operation");
    }
}
