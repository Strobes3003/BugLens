package com.buglens.workspace.exception;

public class WorkspaceUserNotFoundException extends RuntimeException {

    public WorkspaceUserNotFoundException(String email) {
        super("No user exists with email: " + email);
    }
}
