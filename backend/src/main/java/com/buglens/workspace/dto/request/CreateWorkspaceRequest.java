package com.buglens.workspace.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateWorkspaceRequest(
        @NotBlank(message = "Workspace name is required")
        @Size(max = 120, message = "Workspace name must not exceed 120 characters")
        String name
) {
}
