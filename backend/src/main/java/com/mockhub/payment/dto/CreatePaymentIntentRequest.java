package com.mockhub.payment.dto;

import jakarta.validation.constraints.NotBlank;

public record CreatePaymentIntentRequest(
        @NotBlank(message = "Order number is required")
        String orderNumber
) {
}
