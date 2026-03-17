package com.mockhub.payment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Request to create a payment intent")
public record CreatePaymentIntentRequest(
        @NotBlank(message = "Order number is required")
        @Schema(description = "Order number to pay for", example = "ORD-20260317-ABC123")
        String orderNumber
) {
}
