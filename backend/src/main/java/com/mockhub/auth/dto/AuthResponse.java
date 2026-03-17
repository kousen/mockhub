package com.mockhub.auth.dto;

public record AuthResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        UserDto user
) {

    public AuthResponse(String accessToken, long expiresIn, UserDto user) {
        this(accessToken, "Bearer", expiresIn, user);
    }
}
