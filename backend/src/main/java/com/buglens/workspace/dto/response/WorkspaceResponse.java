package com.buglens.workspace.dto.response;

import com.buglens.workspace.entity.Workspace;

import java.time.Instant;
import java.util.List;

public record WorkspaceResponse(
        Long id,
        String name,
        String slug,
        Long createdBy,
        Instant createdAt,
        List<WorkspaceMemberResponse> members
) {

    public static WorkspaceResponse from(Workspace workspace, List<WorkspaceMemberResponse> members) {
        return new WorkspaceResponse(
                workspace.getId(),
                workspace.getName(),
                workspace.getSlug(),
                workspace.getCreatedBy().getId(),
                workspace.getCreatedAt(),
                members
        );
    }
}
