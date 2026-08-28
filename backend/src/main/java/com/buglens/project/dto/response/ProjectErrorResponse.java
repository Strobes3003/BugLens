package com.buglens.project.dto.response;

import java.time.Instant;
import java.util.Map;

public record ProjectErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        Map<String, String> fieldErrors
) {
}
