package com.buglens.dependency.dto.request;

import jakarta.validation.constraints.NotNull;

public record CreateDependencyRequest(
        @NotNull(message = "Blocked issue ID is required")
        Long blockedIssueId
) {
}
