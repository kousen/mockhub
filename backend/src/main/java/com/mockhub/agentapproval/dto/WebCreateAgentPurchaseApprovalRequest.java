package com.mockhub.agentapproval.dto;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record WebCreateAgentPurchaseApprovalRequest(
        @NotBlank String agentId,
        @NotBlank String mandateId,
        @NotBlank String proposedOrderSnapshot,
        String agentRationale,
        @NotNull @DecimalMin("0.00") BigDecimal subtotal,
        @NotNull @DecimalMin("0.00") BigDecimal serviceFee,
        @NotNull @DecimalMin("0.00") BigDecimal total,
        String commercePolicySnapshot,
        Instant expiresAt
) {
}
