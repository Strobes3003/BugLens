package com.buglens.project.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateProjectRequest(
        @NotNull(message = "Workspace ID is required")
        Long workspaceId,

        @NotBlank(message = "Project name is required")
        @Size(max = 120, message = "Project name must not exceed 120 characters")
        String name,

        @Size(max = 10, message = "Project key must not exceed 10 characters")
        String key,

        @Size(max = 2000, message = "Project description must not exceed 2000 characters")
        String description
) {
}
