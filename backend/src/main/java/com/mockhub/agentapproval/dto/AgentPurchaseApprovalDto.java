package com.mockhub.agentapproval.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record AgentPurchaseApprovalDto(
        String approvalId,
        String userEmail,
        String agentId,
        String mandateId,
        String status,
        String proposedOrderSnapshot,
        String agentRationale,
        BigDecimal subtotal,
        BigDecimal serviceFee,
        BigDecimal total,
        String commercePolicySnapshot,
        Instant proposedAt,
        Instant approvedAt,
        Instant deniedAt,
        Instant expiresAt,
        Instant completedAt,
        Instant failedAt,
        String finalOrderNumber,
        String denialReason,
        String failureReason,
        Instant createdAt
) {
}
