package com.buglens.workflow.dto.request;

import com.buglens.issue.entity.IssueStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * @param comment currently accepted but NOT persisted — comment storage arrives in Phase 4 and
 *                activity history in Phase 5. It is validated so the API shape stays stable.
 */
public record TransitionIssueRequest(
        @NotNull(message = "Target status is required")
        IssueStatus targetStatus,

        @Size(max = 2000, message = "Transition comment must not exceed 2000 characters")
        String comment
) {
}
