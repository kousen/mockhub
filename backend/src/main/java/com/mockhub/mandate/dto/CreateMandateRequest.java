package com.mockhub.mandate.dto;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.validation.constraints.NotBlank;

public record CreateMandateRequest(
        @NotBlank String agentId,
        @NotBlank String userEmail,
        @NotBlank String scope,
        BigDecimal maxSpendPerTransaction,
        BigDecimal maxSpendTotal,
        String allowedCategories,
        String allowedEvents,
        String allowedSections,
        String approvalMode,
        Instant expiresAt
) {
    public CreateMandateRequest(
            String agentId,
            String userEmail,
            String scope,
            BigDecimal maxSpendPerTransaction,
            BigDecimal maxSpendTotal,
            String allowedCategories,
            String allowedEvents,
            Instant expiresAt) {
        this(agentId, userEmail, scope, maxSpendPerTransaction, maxSpendTotal,
                allowedCategories, allowedEvents, null, null, expiresAt);
    }
}
