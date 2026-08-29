package com.buglens.workflow.dto.response;

import com.buglens.issue.entity.IssueStatus;

import java.util.List;

public record AllowedTransitionsResponse(
        IssueStatus currentStatus,
        List<IssueStatus> allowedTransitions
) {
}
