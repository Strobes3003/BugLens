package com.buglens.release.dto.response;

import com.buglens.release.entity.Release;
import com.buglens.release.entity.ReleaseStatus;

import java.time.Instant;
import java.time.LocalDate;

public record ReleaseResponse(
        Long id,
        Long projectId,
        String name,
        String description,
        ReleaseStatus status,
        LocalDate targetDate,
        Instant createdAt,
        Instant updatedAt
) {

    public static ReleaseResponse from(Release release) {
        return new ReleaseResponse(
                release.getId(),
                release.getProject().getId(),
                release.getName(),
                release.getDescription(),
                release.getStatus(),
                release.getTargetDate(),
                release.getCreatedAt(),
                release.getUpdatedAt()
        );
    }
}
