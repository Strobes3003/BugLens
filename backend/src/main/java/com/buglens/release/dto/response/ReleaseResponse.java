package com.buglens.release.dto.response;

import com.buglens.release.entity.Release;
import com.buglens.release.entity.ReleaseStatus;

import java.time.Instant;
import java.time.LocalDate;

public record ReleaseResponse(
        Long id,
        Long projectId,
        String name,
        String version,
        String description,
        ReleaseStatus status,
        LocalDate releaseDate,
        Instant createdAt,
        Instant updatedAt
) {

    public static ReleaseResponse from(Release release) {
        return new ReleaseResponse(
                release.getId(),
                release.getProject().getId(),
                release.getName(),
                release.getVersion(),
                release.getDescription(),
                release.getStatus(),
                release.getReleaseDate(),
                release.getCreatedAt(),
                release.getUpdatedAt()
        );
    }
}
