package com.mockhub.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "New user registration data")
public record RegisterRequest(
        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        @Schema(description = "Email address", example = "jane.doe@example.com")
        String email,

        @NotBlank(message = "Password is required")
        @Size(min = 8, max = 100, message = "Password must be between 8 and 100 characters")
        @Schema(description = "Password (8-100 characters)", example = "securepass123")
        String password,

        @NotBlank(message = "First name is required")
        @Size(max = 100, message = "First name must not exceed 100 characters")
        @Schema(description = "First name", example = "Jane")
        String firstName,

        @NotBlank(message = "Last name is required")
        @Size(max = 100, message = "Last name must not exceed 100 characters")
        @Schema(description = "Last name", example = "Doe")
        String lastName,

        @Size(max = 20, message = "Phone must not exceed 20 characters")
        @Schema(description = "Phone number", example = "555-0123")
        String phone
) {
}
