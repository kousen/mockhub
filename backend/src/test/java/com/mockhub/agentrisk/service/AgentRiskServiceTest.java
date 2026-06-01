package com.mockhub.agentrisk.service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import com.mockhub.agentrisk.dto.AgentRiskSummaryDto;
import com.mockhub.agentrisk.dto.RecordAgentRiskSignalRequest;
import com.mockhub.agentrisk.entity.AgentRiskSignal;
import com.mockhub.agentrisk.entity.AgentRiskSignalType;
import com.mockhub.agentrisk.repository.AgentRiskSignalRepository;
import com.mockhub.eval.dto.EvalResult;
import com.mockhub.eval.dto.EvalSeverity;
import com.mockhub.eval.dto.EvalSummary;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AgentRiskServiceTest {

    @Mock
    private AgentRiskSignalRepository agentRiskSignalRepository;

    private AgentRiskService agentRiskService;

    @BeforeEach
    void setUp() {
        agentRiskService = new AgentRiskService(
                agentRiskSignalRepository,
                Duration.ofHours(24),
                Duration.ofMinutes(5),
                3,
                2,
                5,
                new BigDecimal("2000.00"));
    }

    @Test
    @DisplayName("recordSignal - valid request - persists normalized signal")
    void recordSignal_givenValidRequest_persistsNormalizedSignal() {
        when(agentRiskSignalRepository.save(any(AgentRiskSignal.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        agentRiskService.recordSignal(new RecordAgentRiskSignalRequest(
                " buyer@example.com ",
                " agent-1 ",
                AgentRiskSignalType.FAILED_CHECKOUT,
                EvalSeverity.WARNING,
                " CHECKOUT ",
                " ORDER ",
                " MH-1 ",
                " MH-1 ",
                " mandate-1 ",
                new BigDecimal("12.345"),
                " failed "));

        ArgumentCaptor<AgentRiskSignal> captor = ArgumentCaptor.forClass(AgentRiskSignal.class);
        verify(agentRiskSignalRepository).save(captor.capture());
        AgentRiskSignal saved = captor.getValue();
        assertThat(saved.getUserEmail()).isEqualTo("buyer@example.com");
        assertThat(saved.getAgentId()).isEqualTo("agent-1");
        assertThat(saved.getAmount()).isEqualByComparingTo("12.35");
        assertThat(saved.getMessage()).isEqualTo("failed");
    }

    @Test
    @DisplayName("recordCheckoutFailure - oversized audit fields - truncates before persist")
    void recordCheckoutFailure_givenOversizedAuditFields_truncatesBeforePersist() {
        when(agentRiskSignalRepository.save(any(AgentRiskSignal.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        String oversizedOrderNumber = "MH-20260319-0001-" + "x".repeat(120);
        String oversizedMessage = "x".repeat(600);

        agentRiskService.recordCheckoutFailure(
                "buyer@example.com", "agent-1", oversizedOrderNumber, null, oversizedMessage);

        ArgumentCaptor<AgentRiskSignal> captor = ArgumentCaptor.forClass(AgentRiskSignal.class);
        verify(agentRiskSignalRepository).save(captor.capture());
        AgentRiskSignal saved = captor.getValue();
        assertThat(saved.getOrderNumber()).hasSize(30);
        assertThat(saved.getResourceId()).hasSize(100);
        assertThat(saved.getMessage()).hasSize(500);
    }

    @Test
    @DisplayName("recordSignal - oversized identity fields - truncates before persist")
    void recordSignal_givenOversizedIdentityFields_truncatesBeforePersist() {
        when(agentRiskSignalRepository.save(any(AgentRiskSignal.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        agentRiskService.recordSignal(new RecordAgentRiskSignalRequest(
                "u".repeat(300),
                "a".repeat(300),
                AgentRiskSignalType.FAILED_CHECKOUT,
                EvalSeverity.WARNING,
                null,
                null,
                null,
                null,
                null,
                null,
                "message"));

        ArgumentCaptor<AgentRiskSignal> captor = ArgumentCaptor.forClass(AgentRiskSignal.class);
        verify(agentRiskSignalRepository).save(captor.capture());
        AgentRiskSignal saved = captor.getValue();
        assertThat(saved.getUserEmail()).hasSize(255);
        assertThat(saved.getAgentId()).hasSize(255);
    }

    @Test
    @DisplayName("recordSignal - oversized optional audit fields - truncates before persist")
    void recordSignal_givenOversizedOptionalAuditFields_truncatesBeforePersist() {
        when(agentRiskSignalRepository.save(any(AgentRiskSignal.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        agentRiskService.recordSignal(new RecordAgentRiskSignalRequest(
                "buyer@example.com",
                "agent-1",
                AgentRiskSignalType.FAILED_CHECKOUT,
                EvalSeverity.WARNING,
                "a".repeat(80),
                "r".repeat(80),
                "resource-1",
                null,
                "m".repeat(120),
                null,
                "message"));

        ArgumentCaptor<AgentRiskSignal> captor = ArgumentCaptor.forClass(AgentRiskSignal.class);
        verify(agentRiskSignalRepository).save(captor.capture());
        AgentRiskSignal saved = captor.getValue();
        assertThat(saved.getActionType()).hasSize(50);
        assertThat(saved.getResourceType()).hasSize(50);
        assertThat(saved.getMandateId()).hasSize(100);
    }

    @Test
    @DisplayName("recordCheckoutFailure - null message - records fallback message")
    void recordCheckoutFailure_givenNullMessage_recordsFallbackMessage() {
        when(agentRiskSignalRepository.save(any(AgentRiskSignal.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        agentRiskService.recordCheckoutFailure(
                "buyer@example.com", "agent-1", "MH-20260319-0001", null, null);

        ArgumentCaptor<AgentRiskSignal> captor = ArgumentCaptor.forClass(AgentRiskSignal.class);
        verify(agentRiskSignalRepository).save(captor.capture());
        assertThat(captor.getValue().getMessage()).isEqualTo("Checkout failed");
    }

    @Test
    @DisplayName("recordPaymentCredentialFailure - blank message - records fallback message")
    void recordPaymentCredentialFailure_givenBlankMessage_recordsFallbackMessage() {
        when(agentRiskSignalRepository.save(any(AgentRiskSignal.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        agentRiskService.recordPaymentCredentialFailure(
                "buyer@example.com", "agent-1", "MH-20260319-0001", null, " ");

        ArgumentCaptor<AgentRiskSignal> captor = ArgumentCaptor.forClass(AgentRiskSignal.class);
        verify(agentRiskSignalRepository).save(captor.capture());
        assertThat(captor.getValue().getMessage()).isEqualTo("Payment credential validation failed");
    }

    @Test
    @DisplayName("recordSignal - missing user email - rejects request")
    void recordSignal_givenMissingUserEmail_rejectsRequest() {
        RecordAgentRiskSignalRequest request = new RecordAgentRiskSignalRequest(
                " ",
                "agent-1",
                AgentRiskSignalType.FAILED_CHECKOUT,
                EvalSeverity.WARNING,
                null,
                null,
                null,
                null,
                null,
                null,
                "message");

        assertThrows(IllegalArgumentException.class, () -> agentRiskService.recordSignal(request));
    }

    @Test
    @DisplayName("recordCartHoldAttempt - threshold reached - records rapid cart hold warning")
    void recordCartHoldAttempt_givenThresholdReached_recordsRapidCartHoldWarning() {
        when(agentRiskSignalRepository.save(any(AgentRiskSignal.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(agentRiskSignalRepository.countByUserEmailAndAgentIdAndSignalTypeAndCreatedAtAfter(
                eq("buyer@example.com"), eq("agent-1"), eq(AgentRiskSignalType.CART_HOLD_ATTEMPT), any()))
                .thenReturn(5L);

        List<String> warnings = agentRiskService.recordCartHoldAttempt(
                "buyer@example.com", "agent-1", 42L, new BigDecimal("75.00"));

        assertThat(warnings).singleElement().asString().contains("cart hold attempts");
        ArgumentCaptor<AgentRiskSignal> captor = ArgumentCaptor.forClass(AgentRiskSignal.class);
        verify(agentRiskSignalRepository, org.mockito.Mockito.times(2)).save(captor.capture());
        assertThat(captor.getAllValues())
                .extracting(AgentRiskSignal::getSignalType)
                .containsExactly(AgentRiskSignalType.CART_HOLD_ATTEMPT, AgentRiskSignalType.RAPID_CART_HOLD);
    }

    @Test
    @DisplayName("recordHighSpendAttemptIfNeeded - amount above threshold - records warning")
    void recordHighSpendAttemptIfNeeded_givenAmountAboveThreshold_recordsWarning() {
        when(agentRiskSignalRepository.save(any(AgentRiskSignal.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        List<String> warnings = agentRiskService.recordHighSpendAttemptIfNeeded(
                "buyer@example.com", "agent-1", "CHECKOUT", "MH-1", new BigDecimal("2500.00"));

        assertThat(warnings).singleElement().asString().contains("above the risk threshold");
        ArgumentCaptor<AgentRiskSignal> captor = ArgumentCaptor.forClass(AgentRiskSignal.class);
        verify(agentRiskSignalRepository).save(captor.capture());
        assertThat(captor.getValue().getSignalType()).isEqualTo(AgentRiskSignalType.HIGH_SPEND_ATTEMPT);
    }

    @Test
    @DisplayName("recordEvalFailures - mandate failure - records mandate mismatch")
    void recordEvalFailures_givenMandateFailure_recordsMandateMismatch() {
        when(agentRiskSignalRepository.save(any(AgentRiskSignal.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        agentRiskService.recordEvalFailures(
                "buyer@example.com",
                "agent-1",
                "mandate-1",
                "CHECKOUT",
                "ORDER",
                "MH-1",
                new BigDecimal("55.00"),
                new EvalSummary(List.of(EvalResult.fail(
                        "mandate-authorization", EvalSeverity.CRITICAL, "Mandate revoked"))));

        ArgumentCaptor<AgentRiskSignal> captor = ArgumentCaptor.forClass(AgentRiskSignal.class);
        verify(agentRiskSignalRepository).save(captor.capture());
        assertThat(captor.getValue().getSignalType()).isEqualTo(AgentRiskSignalType.MANDATE_MISMATCH);
    }

    @Test
    @DisplayName("summarizeRisk - repeated mandate mismatches - marks blocked")
    void summarizeRisk_givenRepeatedMandateMismatches_marksBlocked() {
        AgentRiskSignal signal = signal(AgentRiskSignalType.MANDATE_MISMATCH, EvalSeverity.CRITICAL);
        when(agentRiskSignalRepository.findRecent(
                eq("buyer@example.com"), eq("agent-1"), any(Instant.class), any(Pageable.class)))
                .thenReturn(List.of(signal));
        when(agentRiskSignalRepository.countByUserEmailAndAgentIdAndCreatedAtAfter(
                eq("buyer@example.com"), eq("agent-1"), any())).thenReturn(3L);
        when(agentRiskSignalRepository.countByUserEmailAndAgentIdAndSeverityAndCreatedAtAfter(
                eq("buyer@example.com"), eq("agent-1"), any(EvalSeverity.class), any()))
                .thenAnswer(invocation -> invocation.getArgument(2) == EvalSeverity.CRITICAL ? 3L : 0L);
        when(agentRiskSignalRepository.countByUserEmailAndAgentIdAndSignalTypeAndCreatedAtAfter(
                eq("buyer@example.com"), eq("agent-1"), any(AgentRiskSignalType.class), any()))
                .thenAnswer(invocation ->
                        invocation.getArgument(2) == AgentRiskSignalType.MANDATE_MISMATCH ? 3L : 0L);

        AgentRiskSummaryDto summary = agentRiskService.summarizeRisk("buyer@example.com", "agent-1");

        assertThat(summary.blocked()).isTrue();
        assertThat(summary.highestSeverity()).isEqualTo("CRITICAL");
        assertThat(summary.reasons()).anySatisfy(reason -> assertThat(reason).contains("Repeated mandate"));
        assertThat(summary.recentSignals()).hasSize(1);
    }

    @Test
    @DisplayName("summarizeRisk - oversized identity fields - queries with truncated values")
    void summarizeRisk_givenOversizedIdentityFields_queriesWithTruncatedValues() {
        String longUserEmail = "u".repeat(300);
        String longAgentId = "a".repeat(300);
        String truncatedUserEmail = "u".repeat(255);
        String truncatedAgentId = "a".repeat(255);

        when(agentRiskSignalRepository.findRecent(
                eq(truncatedUserEmail), eq(truncatedAgentId), any(Instant.class), any(Pageable.class)))
                .thenReturn(List.of());
        when(agentRiskSignalRepository.countByUserEmailAndAgentIdAndCreatedAtAfter(
                eq(truncatedUserEmail), eq(truncatedAgentId), any())).thenReturn(0L);
        when(agentRiskSignalRepository.countByUserEmailAndAgentIdAndSeverityAndCreatedAtAfter(
                eq(truncatedUserEmail), eq(truncatedAgentId), any(EvalSeverity.class), any())).thenReturn(0L);
        when(agentRiskSignalRepository.countByUserEmailAndAgentIdAndSignalTypeAndCreatedAtAfter(
                eq(truncatedUserEmail), eq(truncatedAgentId), any(AgentRiskSignalType.class), any())).thenReturn(0L);

        AgentRiskSummaryDto summary = agentRiskService.summarizeRisk(longUserEmail, longAgentId);

        assertThat(summary.userEmail()).isEqualTo(truncatedUserEmail);
        assertThat(summary.agentId()).isEqualTo(truncatedAgentId);
        verify(agentRiskSignalRepository).findRecent(
                eq(truncatedUserEmail), eq(truncatedAgentId), any(Instant.class), any(Pageable.class));
    }

    private AgentRiskSignal signal(AgentRiskSignalType type, EvalSeverity severity) {
        AgentRiskSignal signal = new AgentRiskSignal();
        signal.setUserEmail("buyer@example.com");
        signal.setAgentId("agent-1");
        signal.setSignalType(type);
        signal.setSeverity(severity);
        signal.setMessage("message");
        signal.setCreatedAt(Instant.now());
        return signal;
    }
}
