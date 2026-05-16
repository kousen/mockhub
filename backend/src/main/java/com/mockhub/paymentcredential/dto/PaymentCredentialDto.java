package com.mockhub.paymentcredential.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record PaymentCredentialDto(
        Long id,
        String credentialId,
        String userEmail,
        String agentId,
        String allowedMerchant,
        BigDecimal maxAmount,
        String currency,
        String usage,
        String status,
        String backingPaymentMethod,
        Instant expiresAt,
        Instant revokedAt,
        Instant consumedAt,
        String consumedByOrderNumber,
        Instant lastUsedAt,
        Instant createdAt
) {
}
