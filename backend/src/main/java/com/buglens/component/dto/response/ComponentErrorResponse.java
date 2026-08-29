package com.buglens.component.dto.response;

import java.time.Instant;
import java.util.Map;

public record ComponentErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        Map<String, String> fieldErrors
) {
}
