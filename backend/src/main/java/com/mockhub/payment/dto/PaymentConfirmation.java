package com.mockhub.payment.dto;

public record PaymentConfirmation(
        String paymentIntentId,
        String status,
        String orderNumber
) {
}
