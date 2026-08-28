package com.buglens.workspace.dto.response;

import com.buglens.workspace.entity.Workspace;
import com.buglens.workspace.entity.WorkspaceMember;
import com.buglens.workspace.entity.WorkspaceRole;

import java.time.Instant;

public record WorkspaceSummaryResponse(
        Long id,
        String name,
        String slug,
        WorkspaceRole role,
        Instant createdAt
) {

    public static WorkspaceSummaryResponse from(WorkspaceMember membership) {
        Workspace workspace = membership.getWorkspace();
        return new WorkspaceSummaryResponse(
                workspace.getId(),
                workspace.getName(),
                workspace.getSlug(),
                membership.getRole(),
                workspace.getCreatedAt()
        );
    }
}
