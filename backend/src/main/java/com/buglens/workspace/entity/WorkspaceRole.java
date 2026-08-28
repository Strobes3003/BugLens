package com.buglens.workspace.entity;

public enum WorkspaceRole {
    OWNER,
    ADMIN,
    MEMBER,
    VIEWER;

    public boolean canManageMembers() {
        return this == OWNER || this == ADMIN;
    }
}
