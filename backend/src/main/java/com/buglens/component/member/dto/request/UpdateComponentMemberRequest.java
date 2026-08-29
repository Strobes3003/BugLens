package com.buglens.component.member.dto.request;

import com.buglens.component.member.entity.ComponentMemberRole;
import jakarta.validation.constraints.NotNull;

public record UpdateComponentMemberRequest(
        @NotNull(message = "Component member role is required")
        ComponentMemberRole role
) {
}
