package com.buglens.component.dto.request;

import com.buglens.component.entity.ComponentStatus;
import jakarta.validation.constraints.Size;

public record UpdateComponentRequest(
        @Size(max = 120, message = "Component name must not exceed 120 characters")
        String name,

        @Size(max = 2000, message = "Component description must not exceed 2000 characters")
        String description,

        ComponentStatus status
) {
}
