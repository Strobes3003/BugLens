package com.buglens.intelligence.dto.response;

import com.buglens.intelligence.entity.ReleaseRisk;

import java.time.Instant;

public record ReleaseRiskResponse(
        Long releaseId,
        String releaseName,
        int riskScore,
        Instant calculatedAt
) {

    public static ReleaseRiskResponse from(ReleaseRisk risk) {
        return new ReleaseRiskResponse(
                risk.getReleaseId(),
                risk.getRelease().getName(),
                risk.getRiskScore(),
                risk.getCalculatedAt()
        );
    }
}
