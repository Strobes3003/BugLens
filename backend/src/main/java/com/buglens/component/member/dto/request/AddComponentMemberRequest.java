package com.buglens.component.member.dto.request;

import com.buglens.component.member.entity.ComponentMemberRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AddComponentMemberRequest(
        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        String email,

        @NotNull(message = "Component member role is required")
        ComponentMemberRole role
) {
}
