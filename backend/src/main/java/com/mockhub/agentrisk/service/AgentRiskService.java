package com.mockhub.agentrisk.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.mockhub.agentrisk.dto.AgentRiskSignalDto;
import com.mockhub.agentrisk.dto.AgentRiskSummaryDto;
import com.mockhub.agentrisk.dto.RecordAgentRiskSignalRequest;
import com.mockhub.agentrisk.entity.AgentRiskSignal;
import com.mockhub.agentrisk.entity.AgentRiskSignalType;
import com.mockhub.agentrisk.repository.AgentRiskSignalRepository;
import com.mockhub.eval.dto.EvalResult;
import com.mockhub.eval.dto.EvalSeverity;
import com.mockhub.eval.dto.EvalSummary;

@Service
public class AgentRiskService {

    private static final int RECENT_SIGNAL_LIMIT = 20;
    private static final String MANDATE_CONDITION = "mandate-authorization";
    private static final String AGENT_RISK_CONDITION = "agent-risk";

    private final AgentRiskSignalRepository agentRiskSignalRepository;
    private final Duration riskWindow;
    private final Duration rapidActionWindow;
    private final int mandateMismatchBlockThreshold;
    private final int failedCheckoutWarningThreshold;
    private final int rapidCartHoldWarningThreshold;
    private final BigDecimal highSpendThreshold;

    public AgentRiskService(AgentRiskSignalRepository agentRiskSignalRepository,
                            @Value("${mockhub.agent-risk.window:PT24H}") Duration riskWindow,
                            @Value("${mockhub.agent-risk.rapid-action-window:PT5M}") Duration rapidActionWindow,
                            @Value("${mockhub.agent-risk.mandate-mismatch-block-threshold:3}")
                            int mandateMismatchBlockThreshold,
                            @Value("${mockhub.agent-risk.failed-checkout-warning-threshold:2}")
                            int failedCheckoutWarningThreshold,
                            @Value("${mockhub.agent-risk.rapid-cart-hold-warning-threshold:5}")
                            int rapidCartHoldWarningThreshold,
                            @Value("${mockhub.agent-risk.high-spend-threshold:2000}")
                            BigDecimal highSpendThreshold) {
        this.agentRiskSignalRepository = agentRiskSignalRepository;
        this.riskWindow = riskWindow;
        this.rapidActionWindow = rapidActionWindow;
        this.mandateMismatchBlockThreshold = mandateMismatchBlockThreshold;
        this.failedCheckoutWarningThreshold = failedCheckoutWarningThreshold;
        this.rapidCartHoldWarningThreshold = rapidCartHoldWarningThreshold;
        this.highSpendThreshold = highSpendThreshold;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AgentRiskSignalDto recordSignal(RecordAgentRiskSignalRequest request) {
        AgentRiskSignal signal = new AgentRiskSignal();
        signal.setUserEmail(normalizeRequired(request.userEmail(), "User email"));
        signal.setAgentId(normalizeRequired(request.agentId(), "Agent ID"));
        signal.setSignalType(requireNonNull(request.signalType(), "Signal type"));
        signal.setSeverity(request.severity() != null ? request.severity() : EvalSeverity.WARNING);
        signal.setActionType(normalizeOptional(request.actionType()));
        signal.setResourceType(normalizeOptional(request.resourceType()));
        signal.setResourceId(normalizeOptional(request.resourceId()));
        signal.setOrderNumber(normalizeOptional(request.orderNumber()));
        signal.setMandateId(normalizeOptional(request.mandateId()));
        signal.setAmount(normalizeAmount(request.amount()));
        signal.setMessage(normalizeRequired(request.message(), "Message"));
        return toDto(agentRiskSignalRepository.save(signal));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordSignalIfIdentified(RecordAgentRiskSignalRequest request) {
        if (isBlank(request.userEmail()) || isBlank(request.agentId())) {
            return;
        }
        recordSignal(request);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public List<String> recordCartHoldAttempt(String userEmail, String agentId,
                                              Long listingId, BigDecimal amount) {
        if (isBlank(userEmail) || isBlank(agentId)) {
            return List.of();
        }
        recordSignal(new RecordAgentRiskSignalRequest(
                userEmail,
                agentId,
                AgentRiskSignalType.CART_HOLD_ATTEMPT,
                EvalSeverity.INFO,
                "ADD_TO_CART",
                "LISTING",
                listingId != null ? listingId.toString() : null,
                null,
                null,
                amount,
                "Agent added a ticket listing to a cart"));

        Instant since = Instant.now().minus(rapidActionWindow);
        long recentCartHolds = agentRiskSignalRepository
                .countByUserEmailAndAgentIdAndSignalTypeAndCreatedAtAfter(
                        userEmail.strip(), agentId.strip(), AgentRiskSignalType.CART_HOLD_ATTEMPT, since);
        if (recentCartHolds < rapidCartHoldWarningThreshold) {
            return List.of();
        }

        String message = "Agent made " + recentCartHolds + " cart hold attempts within "
                + rapidActionWindow.toMinutes() + " minutes";
        recordSignal(new RecordAgentRiskSignalRequest(
                userEmail,
                agentId,
                AgentRiskSignalType.RAPID_CART_HOLD,
                EvalSeverity.WARNING,
                "ADD_TO_CART",
                "LISTING",
                listingId != null ? listingId.toString() : null,
                null,
                null,
                amount,
                message));
        return List.of("agent-risk: " + message);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public List<String> recordCheckoutAttempt(String userEmail, String agentId,
                                              String orderNumber, BigDecimal amount,
                                              String actionType) {
        if (isBlank(userEmail) || isBlank(agentId)) {
            return List.of();
        }
        recordSignal(new RecordAgentRiskSignalRequest(
                userEmail,
                agentId,
                AgentRiskSignalType.CHECKOUT_ATTEMPT,
                EvalSeverity.INFO,
                actionType,
                "ORDER",
                orderNumber,
                orderNumber,
                null,
                amount,
                "Agent attempted checkout or confirmation"));
        return recordHighSpendAttemptIfNeeded(userEmail, agentId, actionType, orderNumber, amount);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public List<String> recordHighSpendAttemptIfNeeded(String userEmail, String agentId,
                                                       String actionType, String resourceId,
                                                       BigDecimal amount) {
        if (isBlank(userEmail) || isBlank(agentId) || amount == null
                || amount.compareTo(highSpendThreshold) <= 0) {
            return List.of();
        }
        BigDecimal normalizedAmount = normalizeAmount(amount);
        String message = "Agent attempted $" + normalizedAmount
                + ", above the risk threshold of $" + highSpendThreshold;
        recordSignal(new RecordAgentRiskSignalRequest(
                userEmail,
                agentId,
                AgentRiskSignalType.HIGH_SPEND_ATTEMPT,
                EvalSeverity.WARNING,
                actionType,
                "ORDER",
                resourceId,
                resourceId,
                null,
                normalizedAmount,
                message));
        return List.of("agent-risk: " + message);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordEvalFailures(String userEmail, String agentId, String mandateId,
                                   String actionType, String resourceType, String resourceId,
                                   BigDecimal amount, EvalSummary evalSummary) {
        if (evalSummary == null || evalSummary.failures().isEmpty()) {
            return;
        }

        for (EvalResult failure : evalSummary.failures()) {
            AgentRiskSignalType signalType = switch (failure.conditionName()) {
                case MANDATE_CONDITION -> AgentRiskSignalType.MANDATE_MISMATCH;
                case AGENT_RISK_CONDITION -> AgentRiskSignalType.AGENT_RISK_BLOCK;
                default -> AgentRiskSignalType.FAILED_CHECKOUT;
            };
            recordSignalIfIdentified(new RecordAgentRiskSignalRequest(
                    userEmail,
                    agentId,
                    signalType,
                    failure.severity(),
                    actionType,
                    resourceType,
                    resourceId,
                    null,
                    mandateId,
                    amount,
                    failure.message()));
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordCheckoutFailure(String userEmail, String agentId,
                                      String orderNumber, BigDecimal amount,
                                      String message) {
        recordSignalIfIdentified(new RecordAgentRiskSignalRequest(
                userEmail,
                agentId,
                AgentRiskSignalType.FAILED_CHECKOUT,
                EvalSeverity.WARNING,
                "CHECKOUT_FAILURE",
                "ORDER",
                orderNumber,
                orderNumber,
                null,
                amount,
                message));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordMandateMismatch(String userEmail, String agentId, String mandateId,
                                      String actionType, String resourceType, String resourceId,
                                      BigDecimal amount, String message) {
        recordSignalIfIdentified(new RecordAgentRiskSignalRequest(
                userEmail,
                agentId,
                AgentRiskSignalType.MANDATE_MISMATCH,
                EvalSeverity.CRITICAL,
                actionType,
                resourceType,
                resourceId,
                null,
                mandateId,
                amount,
                message));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordPaymentCredentialFailure(String userEmail, String agentId,
                                               String orderNumber, BigDecimal amount,
                                               String message) {
        recordSignalIfIdentified(new RecordAgentRiskSignalRequest(
                userEmail,
                agentId,
                AgentRiskSignalType.PAYMENT_CREDENTIAL_FAILURE,
                EvalSeverity.WARNING,
                "PAYMENT_CREDENTIAL_VALIDATION",
                "ORDER",
                orderNumber,
                orderNumber,
                null,
                amount,
                message));
    }

    @Transactional(readOnly = true)
    public AgentRiskSummaryDto summarizeRisk(String userEmail, String agentId) {
        String normalizedUserEmail = normalizeRequired(userEmail, "User email");
        String normalizedAgentId = normalizeRequired(agentId, "Agent ID");
        Instant since = Instant.now().minus(riskWindow);
        List<AgentRiskSignal> recentSignals = agentRiskSignalRepository.findRecent(
                normalizedUserEmail, normalizedAgentId, since, PageRequest.of(0, RECENT_SIGNAL_LIMIT));

        long totalSignals = agentRiskSignalRepository.countByUserEmailAndAgentIdAndCreatedAtAfter(
                normalizedUserEmail, normalizedAgentId, since);
        long warningSignals = agentRiskSignalRepository.countByUserEmailAndAgentIdAndSeverityAndCreatedAtAfter(
                normalizedUserEmail, normalizedAgentId, EvalSeverity.WARNING, since);
        long criticalSignals = agentRiskSignalRepository.countByUserEmailAndAgentIdAndSeverityAndCreatedAtAfter(
                normalizedUserEmail, normalizedAgentId, EvalSeverity.CRITICAL, since);

        long mandateMismatches = agentRiskSignalRepository
                .countByUserEmailAndAgentIdAndSignalTypeAndCreatedAtAfter(
                        normalizedUserEmail, normalizedAgentId, AgentRiskSignalType.MANDATE_MISMATCH, since);
        long failedCheckouts = agentRiskSignalRepository
                .countByUserEmailAndAgentIdAndSignalTypeAndCreatedAtAfter(
                        normalizedUserEmail, normalizedAgentId, AgentRiskSignalType.FAILED_CHECKOUT, since);
        long rapidCartHolds = agentRiskSignalRepository
                .countByUserEmailAndAgentIdAndSignalTypeAndCreatedAtAfter(
                        normalizedUserEmail, normalizedAgentId, AgentRiskSignalType.RAPID_CART_HOLD, since);
        long highSpendAttempts = agentRiskSignalRepository
                .countByUserEmailAndAgentIdAndSignalTypeAndCreatedAtAfter(
                        normalizedUserEmail, normalizedAgentId, AgentRiskSignalType.HIGH_SPEND_ATTEMPT, since);

        List<String> reasons = new ArrayList<>();
        boolean blocked = false;
        if (mandateMismatches >= mandateMismatchBlockThreshold) {
            blocked = true;
            reasons.add("Repeated mandate mismatches: " + mandateMismatches
                    + " in the last " + riskWindow.toHours() + " hours");
        }
        if (failedCheckouts >= failedCheckoutWarningThreshold) {
            reasons.add("Repeated failed checkouts: " + failedCheckouts
                    + " in the last " + riskWindow.toHours() + " hours");
        }
        if (rapidCartHolds > 0) {
            reasons.add("Rapid cart hold warning signals: " + rapidCartHolds);
        }
        if (highSpendAttempts > 0) {
            reasons.add("High spend attempt signals: " + highSpendAttempts);
        }

        String highestSeverity = recentSignals.stream()
                .map(AgentRiskSignal::getSeverity)
                .max(Comparator.comparingInt(this::severityRank))
                .map(Enum::name)
                .orElse(EvalSeverity.INFO.name());

        return new AgentRiskSummaryDto(
                normalizedUserEmail,
                normalizedAgentId,
                since,
                totalSignals,
                warningSignals,
                criticalSignals,
                highestSeverity,
                blocked,
                reasons,
                recentSignals.stream().map(this::toDto).toList());
    }

    private int severityRank(EvalSeverity severity) {
        return switch (severity) {
            case INFO -> 0;
            case WARNING -> 1;
            case CRITICAL -> 2;
        };
    }

    private AgentRiskSignalDto toDto(AgentRiskSignal signal) {
        return new AgentRiskSignalDto(
                signal.getId(),
                signal.getUserEmail(),
                signal.getAgentId(),
                signal.getSignalType().name(),
                signal.getSeverity().name(),
                signal.getActionType(),
                signal.getResourceType(),
                signal.getResourceId(),
                signal.getOrderNumber(),
                signal.getMandateId(),
                signal.getAmount(),
                signal.getMessage(),
                signal.getCreatedAt());
    }

    private String normalizeRequired(String value, String fieldName) {
        if (isBlank(value)) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.strip();
    }

    private String normalizeOptional(String value) {
        return isBlank(value) ? null : value.strip();
    }

    private BigDecimal normalizeAmount(BigDecimal amount) {
        return Optional.ofNullable(amount)
                .map(value -> value.setScale(2, RoundingMode.HALF_UP))
                .orElse(null);
    }

    private <T> T requireNonNull(T value, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
