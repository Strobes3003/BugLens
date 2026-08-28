package com.buglens.workspace.dto.request;

import com.buglens.workspace.entity.WorkspaceRole;
import jakarta.validation.constraints.NotNull;

public record UpdateWorkspaceMemberRequest(
        @NotNull(message = "Member role is required")
        WorkspaceRole role
) {
}
