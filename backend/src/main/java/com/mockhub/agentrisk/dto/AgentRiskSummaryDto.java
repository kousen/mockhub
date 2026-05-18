package com.mockhub.agentrisk.dto;

import java.time.Instant;
import java.util.List;

public record AgentRiskSummaryDto(
        String userEmail,
        String agentId,
        Instant since,
        long totalSignals,
        long warningSignals,
        long criticalSignals,
        String highestSeverity,
        boolean blocked,
        List<String> reasons,
        List<AgentRiskSignalDto> recentSignals
) {
}
