package com.buglens.auth.dto.response;

import java.time.Instant;
import java.util.Map;

public record AuthErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        Map<String, String> fieldErrors
) {
}
