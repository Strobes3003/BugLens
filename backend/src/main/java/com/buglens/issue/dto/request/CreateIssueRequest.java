package com.buglens.issue.dto.request;

import com.buglens.issue.entity.IssuePriority;
import com.buglens.issue.entity.IssueSeverity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateIssueRequest(
        @NotBlank(message = "Issue title is required")
        @Size(max = 200, message = "Issue title must not exceed 200 characters")
        String title,

        String description,

        @NotNull(message = "Issue priority is required")
        IssuePriority priority,

        @NotNull(message = "Issue severity is required")
        IssueSeverity severity,

        @NotNull(message = "Component ID is required")
        Long componentId,

        Long releaseId,

        Long assigneeId
) {
}
