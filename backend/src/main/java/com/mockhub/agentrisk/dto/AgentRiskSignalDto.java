package com.mockhub.agentrisk.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record AgentRiskSignalDto(
        Long id,
        String userEmail,
        String agentId,
        String signalType,
        String severity,
        String actionType,
        String resourceType,
        String resourceId,
        String orderNumber,
        String mandateId,
        BigDecimal amount,
        String message,
        Instant createdAt
) {
}
