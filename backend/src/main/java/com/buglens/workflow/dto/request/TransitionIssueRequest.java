package com.buglens.workflow.dto.request;

import com.buglens.issue.entity.IssueStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * @param comment optional note explaining the transition. Persisted as an issue comment in the
 *                same transaction as the status change, so a rejected transition stores nothing.
 */
public record TransitionIssueRequest(
        @NotNull(message = "Target status is required")
        IssueStatus targetStatus,

        @Size(max = 2000, message = "Transition comment must not exceed 2000 characters")
        String comment
) {
}
