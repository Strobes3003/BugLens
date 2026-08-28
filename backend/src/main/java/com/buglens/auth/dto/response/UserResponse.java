package com.buglens.auth.dto.response;

import com.buglens.auth.entity.User;

import java.time.Instant;

public record UserResponse(
        Long id,
        String name,
        String email,
        String avatarUrl,
        Instant createdAt,
        Instant updatedAt
) {

    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getAvatarUrl(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}
