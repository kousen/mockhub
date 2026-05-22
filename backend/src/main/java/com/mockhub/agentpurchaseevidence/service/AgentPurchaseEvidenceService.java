package com.mockhub.agentpurchaseevidence.service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mockhub.agentapproval.entity.AgentPurchaseApproval;
import com.mockhub.agentapproval.repository.AgentPurchaseApprovalRepository;
import com.mockhub.agentpurchaseevidence.dto.AgentPurchaseEvidenceDto;
import com.mockhub.agentpurchaseevidence.dto.AgentPurchaseEvidenceDto.AgentPurchaseEvidenceActorStepDto;
import com.mockhub.agentpurchaseevidence.dto.AgentPurchaseEvidenceDto.AgentPurchaseEvidenceApprovalDto;
import com.mockhub.agentpurchaseevidence.dto.AgentPurchaseEvidenceDto.AgentPurchaseEvidenceCheckoutDto;
import com.mockhub.agentpurchaseevidence.dto.AgentPurchaseEvidenceDto.AgentPurchaseEvidenceDispatchDto;
import com.mockhub.agentpurchaseevidence.dto.AgentPurchaseEvidenceDto.AgentPurchaseEvidenceEvalOutcomeDto;
import com.mockhub.agentpurchaseevidence.dto.AgentPurchaseEvidenceDto.AgentPurchaseEvidenceFulfillmentDto;
import com.mockhub.agentpurchaseevidence.dto.AgentPurchaseEvidenceDto.AgentPurchaseEvidenceMandateDto;
import com.mockhub.agentpurchaseevidence.dto.AgentPurchaseEvidenceDto.AgentPurchaseEvidenceOrderDto;
import com.mockhub.agentpurchaseevidence.dto.AgentPurchaseEvidenceDto.AgentPurchaseEvidenceOrderItemDto;
import com.mockhub.agentpurchaseevidence.dto.AgentPurchaseEvidenceDto.AgentPurchaseEvidencePaymentCredentialDto;
import com.mockhub.agentpurchaseevidence.dto.AgentPurchaseEvidenceDto.AgentPurchaseEvidenceRiskSignalDto;
import com.mockhub.agentpurchaseevidence.dto.AgentPurchaseEvidenceDto.AgentPurchaseEvidenceTicketArtifactDto;
import com.mockhub.agentrisk.entity.AgentRiskSignal;
import com.mockhub.agentrisk.repository.AgentRiskSignalRepository;
import com.mockhub.auth.entity.User;
import com.mockhub.common.exception.ResourceNotFoundException;
import com.mockhub.common.exception.UnauthorizedException;
import com.mockhub.eval.dto.EvalSeverity;
import com.mockhub.mandate.entity.Mandate;
import com.mockhub.mandate.repository.MandateRepository;
import com.mockhub.order.entity.Order;
import com.mockhub.order.entity.OrderItem;
import com.mockhub.order.entity.OrderStatus;
import com.mockhub.order.repository.OrderRepository;
import com.mockhub.paymentcredential.entity.PaymentCredential;
import com.mockhub.paymentcredential.repository.PaymentCredentialRepository;
import com.mockhub.ticket.entity.Listing;
import com.mockhub.ticket.entity.Ticket;
import com.mockhub.ticket.service.TicketSigningService;
import com.mockhub.venue.entity.Seat;

@Service
public class AgentPurchaseEvidenceService {

    private static final Duration AGENT_SIGNAL_LOOKBACK = Duration.ofMinutes(15);
    private static final String ORDER_VIEW_TOKEN_TYPE = "order-view";
    private static final String TICKET_VERIFICATION_TOKEN_TYPE = "ticket-verification";
    private static final String QR_SIGNING_KEY_REFERENCE = "mockhub.ticket.signing-secret";
    private static final String NOT_PERSISTED = "NOT_PERSISTED";
    private static final String NOT_APPLICABLE = "NOT_APPLICABLE";
    private static final String PASSED = "PASSED";
    private static final String WARNING = "WARNING";
    private static final String BLOCKED = "BLOCKED";

    private final OrderRepository orderRepository;
    private final MandateRepository mandateRepository;
    private final AgentPurchaseApprovalRepository approvalRepository;
    private final PaymentCredentialRepository paymentCredentialRepository;
    private final AgentRiskSignalRepository agentRiskSignalRepository;
    private final TicketSigningService ticketSigningService;
    private final String orderBaseUrl;

    public AgentPurchaseEvidenceService(OrderRepository orderRepository,
                                        MandateRepository mandateRepository,
                                        AgentPurchaseApprovalRepository approvalRepository,
                                        PaymentCredentialRepository paymentCredentialRepository,
                                        AgentRiskSignalRepository agentRiskSignalRepository,
                                        TicketSigningService ticketSigningService,
                                        @Value("${mockhub.sms.order-base-url}") String orderBaseUrl) {
        this.orderRepository = orderRepository;
        this.mandateRepository = mandateRepository;
        this.approvalRepository = approvalRepository;
        this.paymentCredentialRepository = paymentCredentialRepository;
        this.agentRiskSignalRepository = agentRiskSignalRepository;
        this.ticketSigningService = ticketSigningService;
        this.orderBaseUrl = trimTrailingSlash(orderBaseUrl);
    }

    @Transactional(readOnly = true)
    public AgentPurchaseEvidenceDto getEvidence(String orderNumber, String requesterEmail, boolean admin) {
        Order order = loadOrder(orderNumber);
        verifyAccess(order, requesterEmail, admin);
        return assembleEvidence(order);
    }

    @Transactional(readOnly = true)
    public AgentPurchaseEvidenceDto getEvidenceForUser(String userEmail, String orderNumber) {
        return getEvidence(orderNumber, userEmail, false);
    }

    private Order loadOrder(String orderNumber) {
        String normalizedOrderNumber = required(orderNumber, "Order number");
        return orderRepository.findByOrderNumber(normalizedOrderNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "orderNumber", normalizedOrderNumber));
    }

    private void verifyAccess(Order order, String requesterEmail, boolean admin) {
        if (admin) {
            return;
        }
        String normalizedEmail = required(requesterEmail, "Requester email");
        if (!order.getUser().getEmail().equals(normalizedEmail)) {
            throw new UnauthorizedException("You do not have access to this agent purchase evidence");
        }
    }

    private AgentPurchaseEvidenceDto assembleEvidence(Order order) {
        Mandate mandate = findMandate(order).orElse(null);
        AgentPurchaseApproval approval = approvalRepository
                .findFirstByFinalOrderNumberOrderByCreatedAtDesc(order.getOrderNumber())
                .orElse(null);
        PaymentCredential paymentCredential = paymentCredentialRepository
                .findFirstByConsumedByOrderNumberOrderByConsumedAtDesc(order.getOrderNumber())
                .orElse(null);
        List<AgentRiskSignal> riskSignals = findRiskSignals(order);
        AgentPurchaseEvidenceFulfillmentDto fulfillment = toFulfillmentDto(order);
        List<AgentPurchaseEvidenceEvalOutcomeDto> evalOutcomes =
                toEvalOutcomes(order, mandate, approval, paymentCredential, riskSignals);

        return new AgentPurchaseEvidenceDto(
                order.getOrderNumber(),
                order.getUser().getEmail(),
                order.getAgentId(),
                order.getStatus().name(),
                order.getCreatedAt(),
                toOrderDto(order),
                mandate != null ? toMandateDto(mandate) : null,
                approval != null ? toApprovalDto(approval) : null,
                paymentCredential != null ? toPaymentCredentialDto(paymentCredential) : null,
                toCheckoutDto(order),
                fulfillment,
                riskSignals.stream().map(this::toRiskSignalDto).toList(),
                evalOutcomes,
                toActorTimeline(order, mandate, approval, paymentCredential, riskSignals));
    }

    private Optional<Mandate> findMandate(Order order) {
        if (order.getMandateId() == null || order.getMandateId().isBlank()) {
            return Optional.empty();
        }
        return mandateRepository.findByMandateId(order.getMandateId());
    }

    private List<AgentRiskSignal> findRiskSignals(Order order) {
        if (order.getAgentId() == null || order.getAgentId().isBlank()) {
            return List.of();
        }
        Instant createdAt = Optional.ofNullable(order.getCreatedAt()).orElse(Instant.EPOCH);
        Instant endAt = Optional.ofNullable(order.getConfirmedAt())
                .orElse(Optional.ofNullable(order.getUpdatedAt()).orElse(Instant.now()));
        List<AgentRiskSignal> candidates = agentRiskSignalRepository
                .findByUserEmailAndAgentIdAndCreatedAtBetweenOrderByCreatedAtAsc(
                        order.getUser().getEmail(),
                        order.getAgentId(),
                        createdAt.minus(AGENT_SIGNAL_LOOKBACK),
                        endAt.plus(AGENT_SIGNAL_LOOKBACK));

        Set<String> listingIds = new HashSet<>();
        for (OrderItem item : order.getItems()) {
            listingIds.add(item.getListing().getId().toString());
        }

        return candidates.stream()
                .filter(signal -> belongsToOrderEvidence(signal, order.getOrderNumber(), listingIds))
                .toList();
    }

    private boolean belongsToOrderEvidence(AgentRiskSignal signal, String orderNumber, Set<String> listingIds) {
        if (orderNumber.equals(signal.getOrderNumber())) {
            return true;
        }
        if (orderNumber.equals(signal.getResourceId())) {
            return true;
        }
        if (signal.getResourceId() != null && listingIds.contains(signal.getResourceId())) {
            return true;
        }
        return false;
    }

    private AgentPurchaseEvidenceOrderDto toOrderDto(Order order) {
        return new AgentPurchaseEvidenceOrderDto(
                order.getId(),
                order.getOrderNumber(),
                order.getStatus().name(),
                order.getSubtotal(),
                order.getServiceFee(),
                order.getTotal(),
                order.getPaymentMethod(),
                order.getPaymentIntentId(),
                order.getAgentId(),
                order.getMandateId(),
                order.getConfirmedAt(),
                order.getCreatedAt(),
                order.getItems().stream().map(this::toOrderItemDto).toList());
    }

    private AgentPurchaseEvidenceOrderItemDto toOrderItemDto(OrderItem item) {
        Listing listing = item.getListing();
        Ticket ticket = item.getTicket();
        Seat seat = ticket.getSeat();
        String rowLabel = seat != null && seat.getRow() != null ? seat.getRow().getRowLabel() : null;
        String seatNumber = seat != null ? seat.getSeatNumber() : null;

        return new AgentPurchaseEvidenceOrderItemDto(
                item.getId(),
                listing.getId(),
                ticket.getId(),
                listing.getEvent().getName(),
                listing.getEvent().getSlug(),
                listing.getEvent().getEventDate(),
                listing.getEvent().getVenue() != null ? listing.getEvent().getVenue().getName() : null,
                ticket.getSection() != null ? ticket.getSection().getName() : null,
                rowLabel,
                seatNumber,
                ticket.getTicketType(),
                item.getPricePaid(),
                item.getScannedAt());
    }

    private AgentPurchaseEvidenceMandateDto toMandateDto(Mandate mandate) {
        BigDecimal remainingBudget = mandate.getMaxSpendTotal() != null
                ? mandate.getMaxSpendTotal().subtract(mandate.getTotalSpent())
                : null;
        return new AgentPurchaseEvidenceMandateDto(
                mandate.getMandateId(),
                mandate.getAgentId(),
                mandate.getUserEmail(),
                mandate.getScope(),
                mandate.getMaxSpendPerTransaction(),
                mandate.getMaxSpendTotal(),
                mandate.getTotalSpent(),
                remainingBudget,
                mandate.getAllowedCategories(),
                mandate.getAllowedEvents(),
                mandate.getAllowedSections(),
                mandate.getApprovalMode(),
                mandate.getStatus(),
                mandate.getExpiresAt(),
                mandate.getRevokedAt(),
                mandate.getCreatedAt());
    }

    private AgentPurchaseEvidenceApprovalDto toApprovalDto(AgentPurchaseApproval approval) {
        return new AgentPurchaseEvidenceApprovalDto(
                approval.getApprovalId(),
                approval.getUserEmail(),
                approval.getAgentId(),
                approval.getMandateId(),
                approval.getStatus().name(),
                approval.getProposedOrderSnapshot(),
                approval.getAgentRationale(),
                approval.getSubtotal(),
                approval.getServiceFee(),
                approval.getTotal(),
                approval.getCommercePolicySnapshot(),
                approval.getProposedAt(),
                approval.getApprovedAt(),
                approval.getDeniedAt(),
                approval.getExpiresAt(),
                approval.getCompletedAt(),
                approval.getFailedAt(),
                approval.getFinalOrderNumber(),
                approval.getDenialReason(),
                approval.getFailureReason(),
                approval.getCreatedAt());
    }

    private AgentPurchaseEvidencePaymentCredentialDto toPaymentCredentialDto(PaymentCredential credential) {
        return new AgentPurchaseEvidencePaymentCredentialDto(
                credential.getCredentialId(),
                credential.getUserEmail(),
                credential.getAgentId(),
                credential.getAllowedMerchant(),
                credential.getMaxAmount(),
                credential.getCurrency(),
                credential.getUsage().name(),
                credential.getStatus().name(),
                credential.getBackingPaymentMethod(),
                credential.getExpiresAt(),
                credential.getRevokedAt(),
                credential.getConsumedAt(),
                credential.getConsumedByOrderNumber(),
                credential.getLastUsedAt(),
                credential.getCreatedAt());
    }

    private AgentPurchaseEvidenceCheckoutDto toCheckoutDto(Order order) {
        return new AgentPurchaseEvidenceCheckoutDto(
                order.getOrderNumber(),
                mapOrderStatusToAcpStatus(order.getStatus()),
                order.getPaymentMethod(),
                order.getPaymentIntentId(),
                order.getIdempotencyKey(),
                order.getAgentId() != null && !order.getAgentId().isBlank(),
                order.getCreatedAt(),
                order.getConfirmedAt());
    }

    private AgentPurchaseEvidenceFulfillmentDto toFulfillmentDto(Order order) {
        boolean confirmed = order.getStatus() == OrderStatus.CONFIRMED;
        String orderViewToken = confirmed ? ticketSigningService.generateOrderViewToken(order.getOrderNumber()) : null;
        String publicTicketViewUrl = confirmed ? orderBaseUrl + "/tickets/view?token=" + orderViewToken : null;
        User user = order.getUser();

        return new AgentPurchaseEvidenceFulfillmentDto(
                confirmed,
                publicTicketViewUrl,
                confirmed ? ORDER_VIEW_TOKEN_TYPE : null,
                confirmed ? QR_SIGNING_KEY_REFERENCE : null,
                confirmed ? dispatchDto("EMAIL", user.getEmail(), order.getConfirmedAt()) : null,
                smsDispatchDto(user, order.getConfirmedAt(), confirmed),
                order.getItems().stream()
                        .map(item -> toTicketArtifactDto(item, confirmed, orderViewToken))
                        .toList());
    }

    private AgentPurchaseEvidenceDispatchDto smsDispatchDto(User user, Instant confirmedAt, boolean confirmed) {
        if (!confirmed) {
            return null;
        }
        if (user.getPhone() == null || user.getPhone().isBlank()) {
            return new AgentPurchaseEvidenceDispatchDto(
                    "SMS",
                    NOT_APPLICABLE,
                    null,
                    null,
                    null,
                    "User has no phone number, so no SMS dispatch was attempted");
        }
        return dispatchDto("SMS", user.getPhone(), confirmedAt);
    }

    private AgentPurchaseEvidenceDispatchDto dispatchDto(String channel, String recipient, Instant attemptedAt) {
        return new AgentPurchaseEvidenceDispatchDto(
                channel,
                NOT_PERSISTED,
                recipient,
                attemptedAt,
                null,
                "MockHub sends this notification on confirmation but does not persist provider delivery records");
    }

    private AgentPurchaseEvidenceTicketArtifactDto toTicketArtifactDto(OrderItem item, boolean confirmed,
                                                                       String orderViewToken) {
        String ticketPdfUrl = confirmed
                ? "/api/v1/orders/" + item.getOrder().getOrderNumber()
                        + "/tickets/" + item.getTicket().getId() + "/download"
                : null;
        String qrCodeUrl = confirmed
                ? orderBaseUrl + "/api/v1/tickets/" + item.getOrder().getOrderNumber()
                        + "/" + item.getTicket().getId() + "/qr?token=" + orderViewToken
                : null;

        return new AgentPurchaseEvidenceTicketArtifactDto(
                item.getTicket().getId(),
                item.getId(),
                ticketPdfUrl,
                qrCodeUrl != null ? hostRelativeUrl(qrCodeUrl) : null,
                confirmed ? TICKET_VERIFICATION_TOKEN_TYPE : null,
                item.getScannedAt());
    }

    private AgentPurchaseEvidenceRiskSignalDto toRiskSignalDto(AgentRiskSignal signal) {
        return new AgentPurchaseEvidenceRiskSignalDto(
                signal.getId(),
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

    private List<AgentPurchaseEvidenceEvalOutcomeDto> toEvalOutcomes(Order order,
                                                                     Mandate mandate,
                                                                     AgentPurchaseApproval approval,
                                                                     PaymentCredential credential,
                                                                     List<AgentRiskSignal> riskSignals) {
        List<AgentPurchaseEvidenceEvalOutcomeDto> outcomes = new ArrayList<>();
        addMandateOutcome(outcomes, order, mandate);
        addApprovalOutcome(outcomes, mandate, approval);
        addPaymentCredentialOutcome(outcomes, credential, order);
        addRiskOutcomes(outcomes, riskSignals, order);
        return outcomes;
    }

    private void addMandateOutcome(List<AgentPurchaseEvidenceEvalOutcomeDto> outcomes,
                                   Order order, Mandate mandate) {
        if (order.getMandateId() == null || order.getMandateId().isBlank()) {
            return;
        }
        if (mandate == null) {
            outcomes.add(new AgentPurchaseEvidenceEvalOutcomeDto(
                    "mandate-authorization",
                    WARNING,
                    EvalSeverity.WARNING.name(),
                    "Order references mandate " + order.getMandateId() + ", but no mandate record was found",
                    order.getCreatedAt(),
                    order.getMandateId()));
            return;
        }
        outcomes.add(new AgentPurchaseEvidenceEvalOutcomeDto(
                "mandate-authorization",
                PASSED,
                EvalSeverity.INFO.name(),
                "Order was created with recorded agent and mandate context",
                order.getCreatedAt(),
                mandate.getMandateId()));
    }

    private void addApprovalOutcome(List<AgentPurchaseEvidenceEvalOutcomeDto> outcomes,
                                    Mandate mandate, AgentPurchaseApproval approval) {
        if (approval != null) {
            String status = switch (approval.getStatus()) {
                case COMPLETED -> PASSED;
                case FAILED, DENIED, EXPIRED -> BLOCKED;
                case APPROVED, PROPOSED -> WARNING;
            };
            EvalSeverity severity = severityForOutcomeStatus(status);
            outcomes.add(new AgentPurchaseEvidenceEvalOutcomeDto(
                    "purchase-approval",
                    status,
                    severity.name(),
                    "Purchase approval is " + approval.getStatus().name(),
                    mostRelevantApprovalTimestamp(approval),
                    approval.getApprovalId()));
            return;
        }
        if (mandate != null && "APPROVAL_REQUIRED".equals(mandate.getApprovalMode())) {
            outcomes.add(new AgentPurchaseEvidenceEvalOutcomeDto(
                    "purchase-approval",
                    WARNING,
                    EvalSeverity.WARNING.name(),
                    "Mandate requires approval, but no completed approval record is linked to this order",
                    mandate.getCreatedAt(),
                    mandate.getMandateId()));
        }
    }

    private void addPaymentCredentialOutcome(List<AgentPurchaseEvidenceEvalOutcomeDto> outcomes,
                                             PaymentCredential credential, Order order) {
        if (credential == null) {
            return;
        }
        outcomes.add(new AgentPurchaseEvidenceEvalOutcomeDto(
                "payment-credential",
                PASSED,
                EvalSeverity.INFO.name(),
                "Scoped payment credential was consumed for this order",
                Optional.ofNullable(credential.getConsumedAt()).orElse(credential.getCreatedAt()),
                credential.getCredentialId()));
    }

    private EvalSeverity severityForOutcomeStatus(String status) {
        return switch (status) {
            case PASSED -> EvalSeverity.INFO;
            case BLOCKED -> EvalSeverity.CRITICAL;
            default -> EvalSeverity.WARNING;
        };
    }

    private void addRiskOutcomes(List<AgentPurchaseEvidenceEvalOutcomeDto> outcomes,
                                 List<AgentRiskSignal> riskSignals, Order order) {
        List<AgentRiskSignal> nonInfoSignals = riskSignals.stream()
                .filter(signal -> signal.getSeverity() != EvalSeverity.INFO)
                .toList();
        if (nonInfoSignals.isEmpty() && order.getAgentId() != null) {
            outcomes.add(new AgentPurchaseEvidenceEvalOutcomeDto(
                    "agent-risk",
                    PASSED,
                    EvalSeverity.INFO.name(),
                    "No warning or critical risk signals are linked to this order evidence window",
                    order.getConfirmedAt() != null ? order.getConfirmedAt() : order.getCreatedAt(),
                    order.getAgentId()));
            return;
        }
        for (AgentRiskSignal signal : nonInfoSignals) {
            outcomes.add(new AgentPurchaseEvidenceEvalOutcomeDto(
                    riskRuleName(signal),
                    signal.getSeverity() == EvalSeverity.CRITICAL ? BLOCKED : WARNING,
                    signal.getSeverity().name(),
                    signal.getMessage(),
                    signal.getCreatedAt(),
                    signal.getId() != null ? signal.getId().toString() : null));
        }
    }

    private List<AgentPurchaseEvidenceActorStepDto> toActorTimeline(Order order,
                                                                    Mandate mandate,
                                                                    AgentPurchaseApproval approval,
                                                                    PaymentCredential credential,
                                                                    List<AgentRiskSignal> riskSignals) {
        List<AgentPurchaseEvidenceActorStepDto> steps = new ArrayList<>();
        if (mandate != null) {
            steps.add(new AgentPurchaseEvidenceActorStepDto(
                    "MANDATE_CREATED", "USER", mandate.getUserEmail(), mandate.getCreatedAt(),
                    mandate.getMandateId(), "Mandate created for agent " + mandate.getAgentId()));
        }
        if (credential != null) {
            steps.add(new AgentPurchaseEvidenceActorStepDto(
                    "PAYMENT_CREDENTIAL_ISSUED", "USER", credential.getUserEmail(), credential.getCreatedAt(),
                    credential.getCredentialId(), "Scoped payment credential issued for agent "
                            + credential.getAgentId()));
            if (credential.getConsumedAt() != null) {
                steps.add(new AgentPurchaseEvidenceActorStepDto(
                        "PAYMENT_CREDENTIAL_CONSUMED", "AGENT", credential.getAgentId(),
                        credential.getConsumedAt(), credential.getCredentialId(),
                        "Payment credential consumed for order " + credential.getConsumedByOrderNumber()));
            }
        }
        addApprovalSteps(steps, approval);
        for (AgentRiskSignal signal : riskSignals) {
            steps.add(new AgentPurchaseEvidenceActorStepDto(
                    "RISK_SIGNAL_RECORDED", "AGENT", signal.getAgentId(), signal.getCreatedAt(),
                    signal.getId() != null ? signal.getId().toString() : null,
                    signal.getSignalType().name() + ": " + signal.getMessage()));
        }
        String orderActorType = order.getAgentId() != null ? "AGENT" : "USER";
        String orderActorId = order.getAgentId() != null ? order.getAgentId() : order.getUser().getEmail();
        steps.add(new AgentPurchaseEvidenceActorStepDto(
                "ORDER_CREATED", orderActorType, orderActorId, order.getCreatedAt(),
                order.getOrderNumber(), "Checkout created order " + order.getOrderNumber()));
        if (order.getConfirmedAt() != null) {
            steps.add(new AgentPurchaseEvidenceActorStepDto(
                    "ORDER_CONFIRMED", orderActorType, orderActorId, order.getConfirmedAt(),
                    order.getOrderNumber(), "Payment confirmed and tickets fulfilled"));
        }
        return steps.stream()
                .sorted(Comparator.comparing(
                        AgentPurchaseEvidenceActorStepDto::timestamp,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }

    private void addApprovalSteps(List<AgentPurchaseEvidenceActorStepDto> steps,
                                  AgentPurchaseApproval approval) {
        if (approval == null) {
            return;
        }
        steps.add(new AgentPurchaseEvidenceActorStepDto(
                "PURCHASE_APPROVAL_PROPOSED", "AGENT", approval.getAgentId(), approval.getProposedAt(),
                approval.getApprovalId(), "Agent proposed a purchase for user approval"));
        if (approval.getApprovedAt() != null) {
            steps.add(new AgentPurchaseEvidenceActorStepDto(
                    "PURCHASE_APPROVAL_APPROVED", "USER", approval.getUserEmail(), approval.getApprovedAt(),
                    approval.getApprovalId(), "User approved the proposed purchase"));
        }
        if (approval.getDeniedAt() != null) {
            steps.add(new AgentPurchaseEvidenceActorStepDto(
                    "PURCHASE_APPROVAL_DENIED", "USER", approval.getUserEmail(), approval.getDeniedAt(),
                    approval.getApprovalId(), "User denied the proposed purchase"));
        }
        if (approval.getCompletedAt() != null) {
            steps.add(new AgentPurchaseEvidenceActorStepDto(
                    "PURCHASE_APPROVAL_COMPLETED", "AGENT", approval.getAgentId(), approval.getCompletedAt(),
                    approval.getApprovalId(), "Approved proposal completed as order "
                            + approval.getFinalOrderNumber()));
        }
        if (approval.getFailedAt() != null) {
            steps.add(new AgentPurchaseEvidenceActorStepDto(
                    "PURCHASE_APPROVAL_FAILED", "SYSTEM", "MockHub", approval.getFailedAt(),
                    approval.getApprovalId(), approval.getFailureReason()));
        }
    }

    private Instant mostRelevantApprovalTimestamp(AgentPurchaseApproval approval) {
        if (approval.getCompletedAt() != null) {
            return approval.getCompletedAt();
        }
        if (approval.getFailedAt() != null) {
            return approval.getFailedAt();
        }
        if (approval.getDeniedAt() != null) {
            return approval.getDeniedAt();
        }
        if (approval.getApprovedAt() != null) {
            return approval.getApprovedAt();
        }
        return approval.getProposedAt();
    }

    private String riskRuleName(AgentRiskSignal signal) {
        return switch (signal.getSignalType()) {
            case MANDATE_MISMATCH -> "mandate-authorization";
            case AGENT_RISK_BLOCK -> "agent-risk";
            case PAYMENT_CREDENTIAL_FAILURE -> "payment-credential";
            case HIGH_SPEND_ATTEMPT -> "high-spend-threshold";
            case RAPID_CART_HOLD -> "rapid-cart-hold";
            case FAILED_CHECKOUT -> "checkout-completion";
            case CART_HOLD_ATTEMPT, CHECKOUT_ATTEMPT -> "agent-activity";
        };
    }

    private String mapOrderStatusToAcpStatus(OrderStatus orderStatus) {
        return switch (orderStatus) {
            case PENDING -> "CREATED";
            case CONFIRMED -> "COMPLETED";
            case FAILED, CANCELLED -> "CANCELLED";
        };
    }

    private String required(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.strip();
    }

    private static String trimTrailingSlash(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String trimmed = value.strip();
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }

    private String hostRelativeUrl(String absoluteUrl) {
        String prefix = orderBaseUrl + "/";
        if (absoluteUrl.startsWith(prefix)) {
            return "/" + absoluteUrl.substring(prefix.length());
        }
        return absoluteUrl;
    }
}
