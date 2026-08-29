package com.buglens.activity.dto.response;

import java.time.Instant;

public record ActivityErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message
) {
}
