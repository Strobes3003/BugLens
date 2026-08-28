package com.buglens.project.dto.request;

import com.buglens.project.entity.ProjectStatus;
import jakarta.validation.constraints.Size;

public record UpdateProjectRequest(
        @Size(max = 120, message = "Project name must not exceed 120 characters")
        String name,

        @Size(max = 2000, message = "Project description must not exceed 2000 characters")
        String description,

        ProjectStatus status
) {
}
