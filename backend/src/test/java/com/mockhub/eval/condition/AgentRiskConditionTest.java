package com.mockhub.eval.condition;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.mockhub.agentrisk.dto.AgentRiskSummaryDto;
import com.mockhub.agentrisk.service.AgentRiskService;
import com.mockhub.eval.dto.EvalContext;
import com.mockhub.eval.dto.EvalResult;
import com.mockhub.eval.dto.EvalSeverity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AgentRiskConditionTest {

    @Mock
    private AgentRiskService agentRiskService;

    private AgentRiskCondition condition;

    @BeforeEach
    void setUp() {
        condition = new AgentRiskCondition(agentRiskService);
    }

    @Test
    @DisplayName("name - always - returns agent-risk")
    void name_always_returnsAgentRisk() {
        assertThat(condition.name()).isEqualTo("agent-risk");
    }

    @Test
    @DisplayName("appliesTo - agent context - true")
    void appliesTo_givenAgentContext_returnsTrue() {
        EvalContext context = EvalContext.forAgentAction(
                "agent-1", "buyer@example.com", null, null, null, null, "mandate-1");

        assertThat(condition.appliesTo(context)).isTrue();
    }

    @Test
    @DisplayName("appliesTo - missing agentId or userEmail - false")
    void appliesTo_givenMissingAgentIdOrUserEmail_returnsFalse() {
        EvalContext noAgent = EvalContext.forAgentAction(
                null, "buyer@example.com", null, null, null, null, "mandate-1");
        EvalContext noEmail = EvalContext.forAgentAction(
                "agent-1", null, null, null, null, null, "mandate-1");

        assertThat(condition.appliesTo(noAgent)).isFalse();
        assertThat(condition.appliesTo(noEmail)).isFalse();
    }

    @Test
    @DisplayName("evaluate - blocked summary - returns critical failure")
    void evaluate_givenBlockedSummary_returnsCriticalFailure() {
        EvalContext context = EvalContext.forAgentAction(
                "agent-1", "buyer@example.com", null, null, null, null, "mandate-1");
        when(agentRiskService.summarizeRisk("buyer@example.com", "agent-1"))
                .thenReturn(summary(true, List.of("Repeated mandate mismatches: 3")));

        EvalResult result = condition.evaluate(context);

        assertThat(result.passed()).isFalse();
        assertThat(result.severity()).isEqualTo(EvalSeverity.CRITICAL);
        assertThat(result.message()).contains("risk threshold");
        assertThat(result.message()).contains("Repeated mandate mismatches: 3");
    }

    @Test
    @DisplayName("evaluate - warning reasons - returns warning failure")
    void evaluate_givenWarningReasons_returnsWarningFailure() {
        EvalContext context = EvalContext.forAgentAction(
                "agent-1", "buyer@example.com", null, null, null, null, "mandate-1");
        when(agentRiskService.summarizeRisk("buyer@example.com", "agent-1"))
                .thenReturn(summary(false, List.of("High spend attempt signals: 1", "Failed checkouts: 2")));

        EvalResult result = condition.evaluate(context);

        assertThat(result.passed()).isFalse();
        assertThat(result.severity()).isEqualTo(EvalSeverity.WARNING);
        assertThat(result.message()).contains("Recent agent risk signals");
        assertThat(result.message()).contains("High spend attempt signals: 1; Failed checkouts: 2");
    }

    @Test
    @DisplayName("evaluate - clean summary - passes")
    void evaluate_givenCleanSummary_passes() {
        EvalContext context = EvalContext.forAgentAction(
                "agent-1", "buyer@example.com", null, null, null, null, "mandate-1");
        when(agentRiskService.summarizeRisk("buyer@example.com", "agent-1"))
                .thenReturn(summary(false, List.of()));

        EvalResult result = condition.evaluate(context);

        assertThat(result.passed()).isTrue();
        assertThat(result.conditionName()).isEqualTo("agent-risk");
    }

    private AgentRiskSummaryDto summary(boolean blocked, List<String> reasons) {
        return new AgentRiskSummaryDto(
                "buyer@example.com",
                "agent-1",
                Instant.now().minusSeconds(3600),
                reasons.size(),
                reasons.size(),
                blocked ? 3 : 0,
                blocked ? "CRITICAL" : "WARNING",
                blocked,
                reasons,
                List.of());
    }
}
