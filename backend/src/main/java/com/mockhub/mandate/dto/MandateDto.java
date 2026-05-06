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
        String allowedSections,
        String approvalMode,
        String status,
        Instant expiresAt,
        Instant createdAt
) {
    public MandateDto(
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
            Instant createdAt) {
        this(id, mandateId, agentId, userEmail, scope, maxSpendPerTransaction, maxSpendTotal,
                totalSpent, remainingBudget, allowedCategories, allowedEvents, null, "AUTO_PURCHASE",
                status, expiresAt, createdAt);
    }
}
