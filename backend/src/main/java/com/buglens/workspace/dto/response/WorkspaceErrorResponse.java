package com.buglens.workspace.dto.response;

import java.time.Instant;
import java.util.Map;

public record WorkspaceErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        Map<String, String> fieldErrors
) {
}
