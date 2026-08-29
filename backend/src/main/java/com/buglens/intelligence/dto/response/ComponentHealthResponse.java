package com.buglens.intelligence.dto.response;

import com.buglens.intelligence.entity.ComponentHealth;

import java.time.Instant;

public record ComponentHealthResponse(
        Long componentId,
        String componentName,
        int healthScore,
        Instant calculatedAt
) {

    public static ComponentHealthResponse from(ComponentHealth health) {
        return new ComponentHealthResponse(
                health.getComponentId(),
                health.getComponent().getName(),
                health.getHealthScore(),
                health.getCalculatedAt()
        );
    }
}
