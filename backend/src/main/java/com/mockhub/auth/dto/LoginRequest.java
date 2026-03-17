package com.mockhub.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Login credentials")
public record LoginRequest(
        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        @Schema(description = "User email address", example = "buyer@mockhub.com")
        String email,

        @NotBlank(message = "Password is required")
        @Schema(description = "User password", example = "buyer123")
        String password
) {
}
