package com.mockhub.auth.dto;

import java.time.Instant;
import java.util.Set;

public record UserDto(
        Long id,
        String email,
        String firstName,
        String lastName,
        String phone,
        String avatarUrl,
        boolean emailVerified,
        Set<String> roles,
        Instant createdAt
) {
}
