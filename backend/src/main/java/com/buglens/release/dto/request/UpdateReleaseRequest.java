package com.buglens.release.dto.request;

import com.buglens.release.entity.ReleaseStatus;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record UpdateReleaseRequest(
        @Size(max = 120, message = "Release name must not exceed 120 characters")
        String name,

        @Size(max = 2000, message = "Release description must not exceed 2000 characters")
        String description,

        ReleaseStatus status,

        LocalDate targetDate
) {
}
