package com.mockhub.payment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Request to confirm a payment")
public record ConfirmPaymentRequest(
        @NotBlank(message = "Payment intent ID is required")
        @Schema(description = "Stripe payment intent ID", example = "pi_3abc123def456")
        String paymentIntentId
) {
}
