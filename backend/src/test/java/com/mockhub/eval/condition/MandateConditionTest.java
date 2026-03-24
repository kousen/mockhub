package com.mockhub.eval.condition;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.mockhub.eval.dto.EvalContext;
import com.mockhub.eval.dto.EvalResult;
import com.mockhub.eval.dto.EvalSeverity;
import com.mockhub.mandate.entity.Mandate;
import com.mockhub.mandate.service.MandateService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("MandateCondition")
class MandateConditionTest {

    @Mock
    private MandateService mandateService;

    private MandateCondition condition;

    @BeforeEach
    void setUp() {
        condition = new MandateCondition(mandateService);
    }

    @Test
    @DisplayName("name returns mandate-authorization")
    void name_always_returnsMandateAuthorization() {
        assertThat(condition.name()).isEqualTo("mandate-authorization");
    }

    @Test
    @DisplayName("appliesTo returns true when agentId is present")
    void appliesTo_givenAgentIdPresent_returnsTrue() {
        EvalContext context = EvalContext.forAgentAction("agent-1", "user@example.com",
                null, null, null, null, null);

        assertThat(condition.appliesTo(context)).isTrue();
    }

    @Test
    @DisplayName("appliesTo returns false when agentId is null")
    void appliesTo_givenNoAgentId_returnsFalse() {
        EvalContext context = EvalContext.forEvent(null);

        assertThat(condition.appliesTo(context)).isFalse();
    }

    @Test
    @DisplayName("evaluate fails with CRITICAL when userEmail is null")
    void evaluate_givenNullUserEmail_failsCritical() {
        EvalContext context = EvalContext.forAgentAction("agent-1", null,
                null, null, null, null, null);

        EvalResult result = condition.evaluate(context);

        assertThat(result.passed()).isFalse();
        assertThat(result.severity()).isEqualTo(EvalSeverity.CRITICAL);
        assertThat(result.message()).contains("Cannot determine user email");
    }

    @Test
    @DisplayName("evaluate fails with CRITICAL when no active mandate found")
    void evaluate_givenNoActiveMandate_failsCritical() {
        EvalContext context = EvalContext.forAgentAction("agent-1", "user@example.com",
                null, null, null, null, null);
        when(mandateService.getActiveMandate("agent-1", "user@example.com", null)).thenReturn(null);

        EvalResult result = condition.evaluate(context);

        assertThat(result.passed()).isFalse();
        assertThat(result.severity()).isEqualTo(EvalSeverity.CRITICAL);
        assertThat(result.message()).contains("No active mandate");
        assertThat(result.message()).contains("agent-1");
    }

    @Test
    @DisplayName("evaluate passes when mandate authorizes the action")
    void evaluate_givenValidMandate_passes() {
        EvalContext context = EvalContext.forAgentAction("agent-1", "user@example.com",
                null, null, null, null, null);
        Mandate mandate = new Mandate();
        mandate.setMandateId("m1");
        mandate.setScope("BROWSE");
        when(mandateService.getActiveMandate("agent-1", "user@example.com", null)).thenReturn(mandate);
        when(mandateService.validateAction(eq("agent-1"), eq("user@example.com"),
                eq("BROWSE"), any(), any(), any(), eq(null))).thenReturn(true);

        EvalResult result = condition.evaluate(context);

        assertThat(result.passed()).isTrue();
    }

    @Test
    @DisplayName("evaluate fails when mandate does not authorize the action")
    void evaluate_givenUnauthorizedAction_failsCritical() {
        EvalContext context = EvalContext.forAgentAction("agent-1", "user@example.com",
                null, null, new BigDecimal("500.00"), null, "m1");
        Mandate mandate = new Mandate();
        mandate.setMandateId("m1");
        mandate.setScope("PURCHASE");
        when(mandateService.getActiveMandate("agent-1", "user@example.com", "m1")).thenReturn(mandate);
        when(mandateService.validateAction(eq("agent-1"), eq("user@example.com"),
                eq("PURCHASE"), any(), any(), any(), eq("m1"))).thenReturn(false);

        EvalResult result = condition.evaluate(context);

        assertThat(result.passed()).isFalse();
        assertThat(result.severity()).isEqualTo(EvalSeverity.CRITICAL);
        assertThat(result.message()).contains("does not authorize");
    }

    @Test
    @DisplayName("evaluate determines PURCHASE scope when orderTotal is positive")
    void evaluate_givenPositiveOrderTotal_usesPurchaseScope() {
        EvalContext context = EvalContext.forAgentAction("agent-1", "user@example.com",
                null, null, new BigDecimal("99.00"), null, "m1");
        Mandate mandate = new Mandate();
        mandate.setMandateId("m1");
        mandate.setScope("PURCHASE");
        when(mandateService.getActiveMandate("agent-1", "user@example.com", "m1")).thenReturn(mandate);
        when(mandateService.validateAction(eq("agent-1"), eq("user@example.com"),
                eq("PURCHASE"), eq(new BigDecimal("99.00")), any(), any(), eq("m1"))).thenReturn(true);

        EvalResult result = condition.evaluate(context);

        assertThat(result.passed()).isTrue();
    }

    @Test
    @DisplayName("evaluate determines BROWSE scope when orderTotal is null")
    void evaluate_givenNullOrderTotal_usesBrowseScope() {
        EvalContext context = EvalContext.forAgentAction("agent-2", "other@example.com",
                null, null, null, null, null);
        Mandate mandate = new Mandate();
        mandate.setMandateId("m2");
        mandate.setScope("BROWSE");
        when(mandateService.getActiveMandate("agent-2", "other@example.com", null)).thenReturn(mandate);
        when(mandateService.validateAction(eq("agent-2"), eq("other@example.com"),
                eq("BROWSE"), any(), any(), any(), eq(null))).thenReturn(true);

        EvalResult result = condition.evaluate(context);

        assertThat(result.passed()).isTrue();
        assertThat(result.conditionName()).isEqualTo("mandate-authorization");
    }

    @Test
    @DisplayName("evaluate fails when purchase action is missing mandateId")
    void evaluate_givenPurchaseWithoutMandateId_failsCritical() {
        EvalContext context = EvalContext.forAgentAction("agent-1", "user@example.com",
                null, null, new BigDecimal("99.00"), null, null);

        EvalResult result = condition.evaluate(context);

        assertThat(result.passed()).isFalse();
        assertThat(result.severity()).isEqualTo(EvalSeverity.CRITICAL);
        assertThat(result.message()).contains("Mandate ID is required");
    }
}
