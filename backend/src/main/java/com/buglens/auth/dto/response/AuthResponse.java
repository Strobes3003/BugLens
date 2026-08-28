package com.buglens.auth.dto.response;

public record AuthResponse(
        String accessToken,
        String tokenType,
        UserResponse user
) {
}
