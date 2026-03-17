package com.mockhub.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Request to checkout the current cart")
public record CheckoutRequest(
        @NotBlank(message = "Payment method is required")
        @Schema(description = "Payment method identifier", example = "stripe")
        String paymentMethod
) {
}
