package com.buglens.common.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private final JwtService jwtService = new JwtService(
            "development-secret-that-is-at-least-32-bytes-long",
            60_000
    );

    @Test
    void generatesAndValidatesTokenForUser() {
        UserDetails user = User.withUsername("user@example.com")
                .password("ignored")
                .authorities("USER")
                .build();

        String token = jwtService.generateToken(user);

        assertThat(jwtService.extractUsername(token)).isEqualTo("user@example.com");
        assertThat(jwtService.isTokenValid(token, user)).isTrue();
    }
}
