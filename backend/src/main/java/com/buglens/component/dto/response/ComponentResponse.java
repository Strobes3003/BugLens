package com.buglens.component.dto.response;

import com.buglens.component.entity.Component;
import com.buglens.component.entity.ComponentStatus;

import java.time.Instant;

public record ComponentResponse(
        Long id,
        Long projectId,
        String name,
        String description,
        ComponentStatus status,
        Instant createdAt,
        Instant updatedAt
) {

    public static ComponentResponse from(Component component) {
        return new ComponentResponse(
                component.getId(),
                component.getProject().getId(),
                component.getName(),
                component.getDescription(),
                component.getStatus(),
                component.getCreatedAt(),
                component.getUpdatedAt()
        );
    }
}
