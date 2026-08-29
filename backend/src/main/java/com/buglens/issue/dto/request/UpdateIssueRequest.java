package com.buglens.issue.dto.request;

import com.buglens.issue.entity.IssuePriority;
import com.buglens.issue.entity.IssueSeverity;
import jakarta.validation.constraints.Size;

/**
 * Partial update of an issue's details.
 *
 * <p>A {@code null} field means "leave unchanged", matching the convention used by the
 * workspace, project, component and release modules. Because that convention cannot express
 * "clear this value", {@code clearRelease} and {@code clearAssignee} exist as explicit flags —
 * {@code clearRelease} is how an issue is moved back to the backlog.
 *
 * <p>There is deliberately no {@code status} field: status transitions are owned by the
 * workflow engine, not by detail updates.
 */
public record UpdateIssueRequest(
        @Size(max = 200, message = "Issue title must not exceed 200 characters")
        String title,

        String description,

        IssuePriority priority,

        IssueSeverity severity,

        Long componentId,

        Long releaseId,

        Boolean clearRelease,

        Long assigneeId,

        Boolean clearAssignee
) {
}
