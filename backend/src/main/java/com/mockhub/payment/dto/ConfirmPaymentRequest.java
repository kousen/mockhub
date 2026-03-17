package com.mockhub.payment.dto;

import jakarta.validation.constraints.NotBlank;

public record ConfirmPaymentRequest(
        @NotBlank(message = "Payment intent ID is required")
        String paymentIntentId
) {
}
