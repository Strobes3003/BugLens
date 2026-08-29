package com.buglens.workspace.dto.request;

import jakarta.validation.constraints.Size;

public record UpdateWorkspaceRequest(
        @Size(max = 120, message = "Workspace name must not exceed 120 characters")
        String name
) {
}
