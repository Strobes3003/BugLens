package com.buglens.project.dto.response;

import com.buglens.project.entity.Project;
import com.buglens.project.entity.ProjectStatus;

import java.time.Instant;

public record ProjectResponse(
        Long id,
        Long workspaceId,
        String name,
        String key,
        String description,
        ProjectStatus status,
        Instant createdAt,
        Instant updatedAt
) {

    public static ProjectResponse from(Project project) {
        return new ProjectResponse(
                project.getId(),
                project.getWorkspace().getId(),
                project.getName(),
                project.getKey(),
                project.getDescription(),
                project.getStatus(),
                project.getCreatedAt(),
                project.getUpdatedAt()
        );
    }
}
