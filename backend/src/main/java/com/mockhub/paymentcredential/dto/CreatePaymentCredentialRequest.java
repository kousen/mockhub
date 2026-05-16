package com.mockhub.paymentcredential.dto;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreatePaymentCredentialRequest(
        @NotBlank String userEmail,
        @NotBlank String agentId,
        @NotNull @DecimalMin(value = "0.01") BigDecimal maxAmount,
        String currency,
        String usage,
        String backingPaymentMethod,
        Instant expiresAt
) {
}
