package com.buglens.workflow.dto.response;

import com.buglens.issue.entity.IssueStatus;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record WorkflowErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        List<IssueStatus> allowedTransitions,
        Map<String, String> fieldErrors
) {
}
