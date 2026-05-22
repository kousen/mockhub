package com.mockhub.agentpurchaseevidence.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.mockhub.agentapproval.entity.AgentPurchaseApproval;
import com.mockhub.agentapproval.entity.AgentPurchaseApprovalStatus;
import com.mockhub.agentapproval.repository.AgentPurchaseApprovalRepository;
import com.mockhub.agentpurchaseevidence.dto.AgentPurchaseEvidenceDto;
import com.mockhub.agentrisk.entity.AgentRiskSignal;
import com.mockhub.agentrisk.entity.AgentRiskSignalType;
import com.mockhub.agentrisk.repository.AgentRiskSignalRepository;
import com.mockhub.auth.entity.User;
import com.mockhub.common.exception.UnauthorizedException;
import com.mockhub.eval.dto.EvalSeverity;
import com.mockhub.event.entity.Event;
import com.mockhub.mandate.entity.Mandate;
import com.mockhub.mandate.repository.MandateRepository;
import com.mockhub.order.entity.Order;
import com.mockhub.order.entity.OrderItem;
import com.mockhub.order.entity.OrderStatus;
import com.mockhub.order.repository.OrderRepository;
import com.mockhub.paymentcredential.entity.PaymentCredential;
import com.mockhub.paymentcredential.entity.PaymentCredentialStatus;
import com.mockhub.paymentcredential.entity.PaymentCredentialUsage;
import com.mockhub.paymentcredential.repository.PaymentCredentialRepository;
import com.mockhub.ticket.entity.Listing;
import com.mockhub.ticket.entity.Ticket;
import com.mockhub.ticket.service.TicketSigningService;
import com.mockhub.venue.entity.Section;
import com.mockhub.venue.entity.Venue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AgentPurchaseEvidenceServiceTest {

    private static final String ORDER_NUMBER = "MH-20260522-0001";
    private static final String USER_EMAIL = "buyer@example.com";
    private static final String AGENT_ID = "agent-1";
    private static final String MANDATE_ID = "mandate-1";

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private MandateRepository mandateRepository;

    @Mock
    private AgentPurchaseApprovalRepository approvalRepository;

    @Mock
    private PaymentCredentialRepository paymentCredentialRepository;

    @Mock
    private AgentRiskSignalRepository agentRiskSignalRepository;

    @Mock
    private TicketSigningService ticketSigningService;

    private AgentPurchaseEvidenceService evidenceService;

    @BeforeEach
    void setUp() {
        evidenceService = new AgentPurchaseEvidenceService(
                orderRepository,
                mandateRepository,
                approvalRepository,
                paymentCredentialRepository,
                agentRiskSignalRepository,
                ticketSigningService,
                "https://mockhub.example.com");
    }

    @Test
    @DisplayName("getEvidence - complete agent order - returns populated evidence trail")
    void getEvidence_givenCompleteAgentOrder_returnsPopulatedEvidenceTrail() {
        Order order = confirmedAgentOrder();
        Mandate mandate = mandate();
        AgentPurchaseApproval approval = approval();
        PaymentCredential credential = consumedCredential();
        AgentRiskSignal cartSignal = riskSignal(
                1L, AgentRiskSignalType.CART_HOLD_ATTEMPT, EvalSeverity.INFO, "ADD_TO_CART", "LISTING", "10");
        AgentRiskSignal checkoutSignal = riskSignal(
                2L, AgentRiskSignalType.CHECKOUT_ATTEMPT, EvalSeverity.INFO, "CONFIRM_ORDER", "ORDER", ORDER_NUMBER);

        when(orderRepository.findByOrderNumber(ORDER_NUMBER)).thenReturn(Optional.of(order));
        when(mandateRepository.findByMandateId(MANDATE_ID)).thenReturn(Optional.of(mandate));
        when(approvalRepository.findFirstByFinalOrderNumberOrderByCreatedAtDesc(ORDER_NUMBER))
                .thenReturn(Optional.of(approval));
        when(paymentCredentialRepository.findFirstByConsumedByOrderNumberOrderByConsumedAtDesc(ORDER_NUMBER))
                .thenReturn(Optional.of(credential));
        when(agentRiskSignalRepository.findByUserEmailAndAgentIdAndCreatedAtBetweenOrderByCreatedAtAsc(
                any(), any(), any(), any()))
                .thenReturn(List.of(cartSignal, checkoutSignal));
        when(ticketSigningService.generateOrderViewToken(ORDER_NUMBER)).thenReturn("order-view-token");

        AgentPurchaseEvidenceDto evidence = evidenceService.getEvidence(ORDER_NUMBER, USER_EMAIL, false);

        assertThat(evidence.orderNumber()).isEqualTo(ORDER_NUMBER);
        assertThat(evidence.mandate().mandateId()).isEqualTo(MANDATE_ID);
        assertThat(evidence.approval().status()).isEqualTo("COMPLETED");
        assertThat(evidence.paymentCredential().status()).isEqualTo("CONSUMED");
        assertThat(evidence.checkout().acpStatus()).isEqualTo("COMPLETED");
        assertThat(evidence.fulfillment().ticketPdfAvailable()).isTrue();
        assertThat(evidence.fulfillment().publicTicketViewUrl()).contains("order-view-token");
        assertThat(evidence.fulfillment().ticketArtifacts()).hasSize(1);
        assertThat(evidence.fulfillment().ticketArtifacts().getFirst().qrCodeUrl())
                .startsWith("/api/v1/tickets/");
        assertThat(evidence.riskSignals()).hasSize(2);
        assertThat(evidence.evalOutcomes())
                .extracting(AgentPurchaseEvidenceDto.AgentPurchaseEvidenceEvalOutcomeDto::ruleName)
                .contains("mandate-authorization", "payment-credential", "agent-risk");
        assertThat(evidence.actorTimeline())
                .extracting(AgentPurchaseEvidenceDto.AgentPurchaseEvidenceActorStepDto::step)
                .contains("MANDATE_CREATED", "ORDER_CONFIRMED", "PAYMENT_CREDENTIAL_CONSUMED");
    }

    @Test
    @DisplayName("getEvidence - risk resourceId matches order - includes only order-specific signals")
    void getEvidence_givenRiskResourceIdMatchesOrder_includesOnlyOrderSpecificSignals() {
        Order order = confirmedAgentOrder();
        AgentRiskSignal blockedConfirmation = riskSignal(
                3L, AgentRiskSignalType.MANDATE_MISMATCH, EvalSeverity.CRITICAL,
                "ACP_CONFIRMATION_VALIDATION", "ORDER", ORDER_NUMBER);
        blockedConfirmation.setOrderNumber(null);
        AgentRiskSignal genericCheckout = riskSignal(
                4L, AgentRiskSignalType.CHECKOUT_ATTEMPT, EvalSeverity.INFO,
                "CHECKOUT", "ORDER", null);
        genericCheckout.setOrderNumber(null);
        genericCheckout.setResourceId(null);

        when(orderRepository.findByOrderNumber(ORDER_NUMBER)).thenReturn(Optional.of(order));
        when(mandateRepository.findByMandateId(MANDATE_ID)).thenReturn(Optional.empty());
        when(approvalRepository.findFirstByFinalOrderNumberOrderByCreatedAtDesc(ORDER_NUMBER))
                .thenReturn(Optional.empty());
        when(paymentCredentialRepository.findFirstByConsumedByOrderNumberOrderByConsumedAtDesc(ORDER_NUMBER))
                .thenReturn(Optional.empty());
        when(agentRiskSignalRepository.findByUserEmailAndAgentIdAndCreatedAtBetweenOrderByCreatedAtAsc(
                any(), any(), any(), any()))
                .thenReturn(List.of(blockedConfirmation, genericCheckout));
        when(ticketSigningService.generateOrderViewToken(ORDER_NUMBER)).thenReturn("order-view-token");

        AgentPurchaseEvidenceDto evidence = evidenceService.getEvidence(ORDER_NUMBER, USER_EMAIL, false);

        assertThat(evidence.riskSignals())
                .extracting(AgentPurchaseEvidenceDto.AgentPurchaseEvidenceRiskSignalDto::id)
                .containsExactly(3L);
        assertThat(evidence.evalOutcomes())
                .anyMatch(outcome -> outcome.ruleName().equals("mandate-authorization")
                        && outcome.status().equals("BLOCKED"));
    }

    @Test
    @DisplayName("getEvidence - non-owner without admin role - throws UnauthorizedException")
    void getEvidence_givenNonOwnerWithoutAdmin_throwsUnauthorizedException() {
        Order order = confirmedAgentOrder();
        when(orderRepository.findByOrderNumber(ORDER_NUMBER)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> evidenceService.getEvidence(ORDER_NUMBER, "other@example.com", false))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("agent purchase evidence");
    }

    @Test
    @DisplayName("getEvidence - admin requester - bypasses owner check")
    void getEvidence_givenAdminRequester_bypassesOwnerCheck() {
        Order order = pendingAgentOrder();
        when(orderRepository.findByOrderNumber(ORDER_NUMBER)).thenReturn(Optional.of(order));
        when(mandateRepository.findByMandateId(MANDATE_ID)).thenReturn(Optional.empty());
        when(approvalRepository.findFirstByFinalOrderNumberOrderByCreatedAtDesc(ORDER_NUMBER))
                .thenReturn(Optional.empty());
        when(paymentCredentialRepository.findFirstByConsumedByOrderNumberOrderByConsumedAtDesc(ORDER_NUMBER))
                .thenReturn(Optional.empty());
        when(agentRiskSignalRepository.findByUserEmailAndAgentIdAndCreatedAtBetweenOrderByCreatedAtAsc(
                any(), any(), any(), any()))
                .thenReturn(List.of());

        AgentPurchaseEvidenceDto evidence = evidenceService.getEvidence(ORDER_NUMBER, "admin@example.com", true);

        assertThat(evidence.mandate()).isNull();
        assertThat(evidence.paymentCredential()).isNull();
        assertThat(evidence.fulfillment().ticketPdfAvailable()).isFalse();
        assertThat(evidence.evalOutcomes())
                .anyMatch(outcome -> outcome.ruleName().equals("mandate-authorization")
                        && outcome.status().equals("WARNING"));
        verify(orderRepository).findByOrderNumber(ORDER_NUMBER);
    }

    private Order confirmedAgentOrder() {
        Order order = pendingAgentOrder();
        order.setStatus(OrderStatus.CONFIRMED);
        order.setConfirmedAt(Instant.parse("2026-05-22T15:00:00Z"));
        order.setPaymentIntentId("mock_pi_123");
        order.setItems(new ArrayList<>(List.of(orderItem(order))));
        return order;
    }

    private Order pendingAgentOrder() {
        User user = user();
        Order order = new Order();
        order.setId(99L);
        order.setUser(user);
        order.setOrderNumber(ORDER_NUMBER);
        order.setStatus(OrderStatus.PENDING);
        order.setSubtotal(new BigDecimal("85.00"));
        order.setServiceFee(new BigDecimal("8.50"));
        order.setTotal(new BigDecimal("93.50"));
        order.setPaymentMethod("mock");
        order.setAgentId(AGENT_ID);
        order.setMandateId(MANDATE_ID);
        order.setCreatedAt(Instant.parse("2026-05-22T14:55:00Z"));
        order.setUpdatedAt(Instant.parse("2026-05-22T14:56:00Z"));
        return order;
    }

    private OrderItem orderItem(Order order) {
        Venue venue = new Venue();
        venue.setName("Test Arena");

        Event event = new Event();
        event.setName("Test Concert");
        event.setSlug("test-concert");
        event.setEventDate(Instant.parse("2026-06-22T00:00:00Z"));
        event.setVenue(venue);

        Listing listing = new Listing();
        listing.setId(10L);
        listing.setEvent(event);

        Section section = new Section();
        section.setName("Floor");

        Ticket ticket = new Ticket();
        ticket.setId(20L);
        ticket.setSection(section);
        ticket.setTicketType("STANDARD");

        OrderItem item = new OrderItem();
        item.setId(30L);
        item.setOrder(order);
        item.setListing(listing);
        item.setTicket(ticket);
        item.setPricePaid(new BigDecimal("85.00"));
        return item;
    }

    private User user() {
        User user = new User();
        user.setId(1L);
        user.setEmail(USER_EMAIL);
        user.setPasswordHash("hash");
        user.setPhone("+15551234567");
        return user;
    }

    private Mandate mandate() {
        Mandate mandate = new Mandate();
        mandate.setMandateId(MANDATE_ID);
        mandate.setAgentId(AGENT_ID);
        mandate.setUserEmail(USER_EMAIL);
        mandate.setScope("PURCHASE");
        mandate.setMaxSpendPerTransaction(new BigDecimal("500.00"));
        mandate.setMaxSpendTotal(new BigDecimal("2000.00"));
        mandate.setTotalSpent(new BigDecimal("93.50"));
        mandate.setApprovalMode("APPROVAL_REQUIRED");
        mandate.setStatus("ACTIVE");
        mandate.setCreatedAt(Instant.parse("2026-05-22T14:50:00Z"));
        return mandate;
    }

    private AgentPurchaseApproval approval() {
        AgentPurchaseApproval approval = new AgentPurchaseApproval();
        approval.setApprovalId("approval-1");
        approval.setUserEmail(USER_EMAIL);
        approval.setAgentId(AGENT_ID);
        approval.setMandateId(MANDATE_ID);
        approval.setStatus(AgentPurchaseApprovalStatus.COMPLETED);
        approval.setProposedOrderSnapshot("{\"listingId\":10}");
        approval.setSubtotal(new BigDecimal("85.00"));
        approval.setServiceFee(new BigDecimal("8.50"));
        approval.setTotal(new BigDecimal("93.50"));
        approval.setProposedAt(Instant.parse("2026-05-22T14:57:00Z"));
        approval.setApprovedAt(Instant.parse("2026-05-22T14:58:00Z"));
        approval.setCompletedAt(Instant.parse("2026-05-22T15:00:01Z"));
        approval.setFinalOrderNumber(ORDER_NUMBER);
        approval.setCreatedAt(Instant.parse("2026-05-22T14:57:00Z"));
        return approval;
    }

    private PaymentCredential consumedCredential() {
        PaymentCredential credential = new PaymentCredential();
        credential.setCredentialId("credential-1");
        credential.setUserEmail(USER_EMAIL);
        credential.setAgentId(AGENT_ID);
        credential.setAllowedMerchant("MOCKHUB");
        credential.setMaxAmount(new BigDecimal("100.00"));
        credential.setCurrency("USD");
        credential.setUsage(PaymentCredentialUsage.ONE_TIME);
        credential.setStatus(PaymentCredentialStatus.CONSUMED);
        credential.setBackingPaymentMethod("mock");
        credential.setConsumedAt(Instant.parse("2026-05-22T14:59:59Z"));
        credential.setConsumedByOrderNumber(ORDER_NUMBER);
        credential.setCreatedAt(Instant.parse("2026-05-22T14:58:30Z"));
        return credential;
    }

    private AgentRiskSignal riskSignal(Long id, AgentRiskSignalType type, EvalSeverity severity,
                                       String actionType, String resourceType, String resourceId) {
        AgentRiskSignal signal = new AgentRiskSignal();
        signal.setId(id);
        signal.setUserEmail(USER_EMAIL);
        signal.setAgentId(AGENT_ID);
        signal.setSignalType(type);
        signal.setSeverity(severity);
        signal.setActionType(actionType);
        signal.setResourceType(resourceType);
        signal.setResourceId(resourceId);
        signal.setOrderNumber(ORDER_NUMBER.equals(resourceId) ? ORDER_NUMBER : null);
        signal.setMandateId(MANDATE_ID);
        signal.setAmount(new BigDecimal("93.50"));
        signal.setMessage(type.name());
        signal.setCreatedAt(Instant.parse("2026-05-22T14:56:00Z"));
        return signal;
    }
}
