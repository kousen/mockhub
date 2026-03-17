package com.mockhub.order.dto;

import jakarta.validation.constraints.NotBlank;

public record CheckoutRequest(
        @NotBlank(message = "Payment method is required")
        String paymentMethod
) {
}
