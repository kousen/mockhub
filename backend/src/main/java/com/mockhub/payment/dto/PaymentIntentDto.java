package com.mockhub.payment.dto;

import java.math.BigDecimal;

public record PaymentIntentDto(
        String paymentIntentId,
        String clientSecret,
        BigDecimal amount,
        String currency
) {
}
