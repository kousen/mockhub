package com.mockhub.agentrisk.dto;

import java.math.BigDecimal;

import com.mockhub.agentrisk.entity.AgentRiskSignalType;
import com.mockhub.eval.dto.EvalSeverity;

public record RecordAgentRiskSignalRequest(
        String userEmail,
        String agentId,
        AgentRiskSignalType signalType,
        EvalSeverity severity,
        String actionType,
        String resourceType,
        String resourceId,
        String orderNumber,
        String mandateId,
        BigDecimal amount,
        String message
) {
}
