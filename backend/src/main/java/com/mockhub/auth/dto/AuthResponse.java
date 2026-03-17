package com.mockhub.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Authentication response with JWT token")
public record AuthResponse(
        @Schema(description = "JWT access token", example = "eyJhbGciOiJIUzI1NiJ9...")
        String accessToken,
        @Schema(description = "Token type", example = "Bearer")
        String tokenType,
        @Schema(description = "Token expiration in seconds", example = "3600")
        long expiresIn,
        @Schema(description = "Authenticated user details")
        UserDto user
) {

    public AuthResponse(String accessToken, long expiresIn, UserDto user) {
        this(accessToken, "Bearer", expiresIn, user);
    }
}
