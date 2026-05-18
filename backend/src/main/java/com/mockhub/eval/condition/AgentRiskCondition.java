package com.mockhub.eval.condition;

import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.mockhub.agentrisk.dto.AgentRiskSummaryDto;
import com.mockhub.agentrisk.service.AgentRiskService;
import com.mockhub.eval.EvalCondition;
import com.mockhub.eval.dto.EvalContext;
import com.mockhub.eval.dto.EvalResult;
import com.mockhub.eval.dto.EvalSeverity;

@Component
public class AgentRiskCondition implements EvalCondition {

    private final AgentRiskService agentRiskService;

    public AgentRiskCondition(AgentRiskService agentRiskService) {
        this.agentRiskService = agentRiskService;
    }

    @Override
    public String name() {
        return "agent-risk";
    }

    @Override
    public boolean appliesTo(EvalContext context) {
        return context.agentId() != null && context.userEmail() != null;
    }

    @Override
    public EvalResult evaluate(EvalContext context) {
        AgentRiskSummaryDto summary = agentRiskService.summarizeRisk(context.userEmail(), context.agentId());
        if (summary.blocked()) {
            return EvalResult.fail(name(), EvalSeverity.CRITICAL,
                    "Agent risk threshold exceeded: " + formatReasons(summary));
        }
        if (!summary.reasons().isEmpty()) {
            return EvalResult.fail(name(), EvalSeverity.WARNING,
                    "Recent agent risk signals: " + formatReasons(summary));
        }
        return EvalResult.pass(name());
    }

    private String formatReasons(AgentRiskSummaryDto summary) {
        return summary.reasons().stream().collect(Collectors.joining("; "));
    }
}
