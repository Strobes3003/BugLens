package com.buglens.release.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record CreateReleaseRequest(
        @NotNull(message = "Project ID is required")
        Long projectId,

        @NotBlank(message = "Release name is required")
        @Size(max = 120, message = "Release name must not exceed 120 characters")
        String name,

        @NotBlank(message = "Release version is required")
        @Size(max = 50, message = "Release version must not exceed 50 characters")
        String version,

        @Size(max = 2000, message = "Release description must not exceed 2000 characters")
        String description,

        LocalDate releaseDate
) {
}
