package com.buglens.release.dto.response;

import java.time.Instant;
import java.util.Map;

public record ReleaseErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        Map<String, String> fieldErrors
) {
}
