package com.buglens.issue.dto.response;

import java.time.Instant;
import java.util.Map;

public record IssueErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        Map<String, String> fieldErrors
) {
}
