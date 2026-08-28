package com.buglens.workspace.dto.request;

import com.buglens.workspace.entity.WorkspaceRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AddWorkspaceMemberRequest(
        @NotBlank(message = "Member email is required")
        @Email(message = "Member email must be valid")
        String email,

        @NotNull(message = "Member role is required")
        WorkspaceRole role
) {
}
