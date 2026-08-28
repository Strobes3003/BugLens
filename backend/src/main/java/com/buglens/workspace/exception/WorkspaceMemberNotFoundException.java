package com.buglens.workspace.exception;

public class WorkspaceMemberNotFoundException extends RuntimeException {

    public WorkspaceMemberNotFoundException(Long userId) {
        super("User is not a member of this workspace: " + userId);
    }
}
