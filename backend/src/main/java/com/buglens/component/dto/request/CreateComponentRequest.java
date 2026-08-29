package com.buglens.component.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateComponentRequest(
        @NotNull(message = "Project ID is required")
        Long projectId,

        @NotBlank(message = "Component name is required")
        @Size(max = 120, message = "Component name must not exceed 120 characters")
        String name,

        @Size(max = 2000, message = "Component description must not exceed 2000 characters")
        String description
) {
}
