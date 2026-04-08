package com.mockhub.mandate.dto;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.validation.constraints.NotBlank;

public record WebCreateMandateRequest(
        @NotBlank String agentId,
        @NotBlank String scope,
        BigDecimal maxSpendPerTransaction,
        BigDecimal maxSpendTotal,
        String allowedCategories,
        String allowedEvents,
        Instant expiresAt
) {}
