package com.mockhub.mandate.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record MandateDto(
        Long id,
        String mandateId,
        String agentId,
        String userEmail,
        String scope,
        BigDecimal maxSpendPerTransaction,
        BigDecimal maxSpendTotal,
        BigDecimal totalSpent,
        BigDecimal remainingBudget,
        String allowedCategories,
        String allowedEvents,
        String status,
        Instant expiresAt,
        Instant createdAt
) {}
