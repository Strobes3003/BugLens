package com.buglens.intelligence.dto.response;

import java.time.Instant;

public record IntelligenceErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message
) {
}
