package com.buglens.dependency.dto.response;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record DependencyErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        List<Long> cyclePath,
        Map<String, String> fieldErrors
) {
}
