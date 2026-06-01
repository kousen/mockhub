package com.mockhub.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Request to checkout the current cart")
public record CheckoutRequest(
        @NotBlank(message = "Payment method is required")
        @Size(max = 30, message = "Payment method must not exceed 30 characters")
        @Pattern(regexp = "(?i)\\s*(mock|stripe)\\s*", message = "Payment method must be one of: mock, stripe")
        @Schema(description = "Payment method identifier", example = "mock", allowableValues = {"mock", "stripe"})
        String paymentMethod
) {
}
