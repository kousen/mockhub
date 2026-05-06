package com.mockhub.agentapproval.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.mockhub.agentapproval.dto.AgentPurchaseApprovalDto;
import com.mockhub.agentapproval.dto.CreateAgentPurchaseApprovalRequest;
import com.mockhub.agentapproval.entity.AgentPurchaseApproval;
import com.mockhub.agentapproval.entity.AgentPurchaseApprovalStatus;
import com.mockhub.agentapproval.repository.AgentPurchaseApprovalRepository;
import com.mockhub.common.exception.ConflictException;
import com.mockhub.common.exception.UnauthorizedException;
import com.mockhub.order.entity.Order;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AgentPurchaseApprovalServiceTest {

    private static final String USER_EMAIL = "buyer@example.com";
    private static final String AGENT_ID = "shopping-agent";
    private static final String MANDATE_ID = "mandate-123";

    @Mock
    private AgentPurchaseApprovalRepository approvalRepository;

    private AgentPurchaseApprovalService approvalService;

    @BeforeEach
    void setUp() {
        approvalService = new AgentPurchaseApprovalService(approvalRepository);
    }

    @Test
    void createProposal_givenValidRequest_persistsProposedApproval() {
        when(approvalRepository.save(any(AgentPurchaseApproval.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AgentPurchaseApprovalDto dto = approvalService.createProposal(createRequest());

        assertNotNull(dto.approvalId());
        assertEquals(USER_EMAIL, dto.userEmail());
        assertEquals("PROPOSED", dto.status());
        assertEquals(new BigDecimal("55.00"), dto.total());

        ArgumentCaptor<AgentPurchaseApproval> captor = ArgumentCaptor.forClass(AgentPurchaseApproval.class);
        verify(approvalRepository).save(captor.capture());
        assertEquals(AgentPurchaseApprovalStatus.PROPOSED, captor.getValue().getStatus());
    }

    @Test
    void approve_givenProposedApproval_marksApproved() {
        AgentPurchaseApproval approval = approval("approval-123", AgentPurchaseApprovalStatus.PROPOSED);
        when(approvalRepository.findByApprovalIdForUpdate("approval-123")).thenReturn(Optional.of(approval));
        when(approvalRepository.save(any(AgentPurchaseApproval.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AgentPurchaseApprovalDto dto = approvalService.approve("approval-123", USER_EMAIL);

        assertEquals("APPROVED", dto.status());
        assertNotNull(dto.approvedAt());
    }

    @Test
    void deny_givenProposedApproval_recordsReason() {
        AgentPurchaseApproval approval = approval("approval-123", AgentPurchaseApprovalStatus.PROPOSED);
        when(approvalRepository.findByApprovalIdForUpdate("approval-123")).thenReturn(Optional.of(approval));
        when(approvalRepository.save(any(AgentPurchaseApproval.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AgentPurchaseApprovalDto dto = approvalService.deny("approval-123", USER_EMAIL, "Too expensive");

        assertEquals("DENIED", dto.status());
        assertEquals("Too expensive", dto.denialReason());
    }

    @Test
    void approve_givenDifferentUser_throwsUnauthorizedException() {
        AgentPurchaseApproval approval = approval("approval-123", AgentPurchaseApprovalStatus.PROPOSED);
        when(approvalRepository.findByApprovalIdForUpdate("approval-123")).thenReturn(Optional.of(approval));

        assertThrows(UnauthorizedException.class,
                () -> approvalService.approve("approval-123", "other@example.com"));
    }

    @Test
    void approve_givenExpiredApproval_marksExpiredAndThrowsConflictException() {
        AgentPurchaseApproval approval = approval("approval-123", AgentPurchaseApprovalStatus.PROPOSED);
        approval.setExpiresAt(Instant.now().minusSeconds(60));
        when(approvalRepository.findByApprovalIdForUpdate("approval-123")).thenReturn(Optional.of(approval));
        when(approvalRepository.save(any(AgentPurchaseApproval.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        assertThrows(ConflictException.class, () -> approvalService.approve("approval-123", USER_EMAIL));
        assertEquals(AgentPurchaseApprovalStatus.EXPIRED, approval.getStatus());
    }

    @Test
    void validateApprovedForCompletion_givenMatchingApproval_allowsCompletion() {
        AgentPurchaseApproval approval = approval("approval-123", AgentPurchaseApprovalStatus.APPROVED);
        Order order = order(new BigDecimal("55.00"));
        when(approvalRepository.findByApprovalIdForUpdate("approval-123")).thenReturn(Optional.of(approval));

        approvalService.validateApprovedForCompletion(
                "approval-123", USER_EMAIL, AGENT_ID, MANDATE_ID, order);
    }

    @Test
    void validateApprovedForCompletion_givenDeniedApproval_throwsConflictException() {
        AgentPurchaseApproval approval = approval("approval-123", AgentPurchaseApprovalStatus.DENIED);
        Order order = order(new BigDecimal("55.00"));
        when(approvalRepository.findByApprovalIdForUpdate("approval-123")).thenReturn(Optional.of(approval));

        assertThrows(ConflictException.class, () -> approvalService.validateApprovedForCompletion(
                "approval-123", USER_EMAIL, AGENT_ID, MANDATE_ID, order));
    }

    @Test
    void validateApprovedForCompletion_givenTotalMismatch_throwsConflictException() {
        AgentPurchaseApproval approval = approval("approval-123", AgentPurchaseApprovalStatus.APPROVED);
        Order order = order(new BigDecimal("60.00"));
        when(approvalRepository.findByApprovalIdForUpdate("approval-123")).thenReturn(Optional.of(approval));

        assertThrows(ConflictException.class, () -> approvalService.validateApprovedForCompletion(
                "approval-123", USER_EMAIL, AGENT_ID, MANDATE_ID, order));
    }

    @Test
    void markCompleted_givenApprovedApproval_recordsFinalOrderNumber() {
        AgentPurchaseApproval approval = approval("approval-123", AgentPurchaseApprovalStatus.APPROVED);
        when(approvalRepository.findByApprovalIdForUpdate("approval-123")).thenReturn(Optional.of(approval));

        approvalService.markCompleted("approval-123", "MH-20260506-0001");

        assertEquals(AgentPurchaseApprovalStatus.COMPLETED, approval.getStatus());
        assertEquals("MH-20260506-0001", approval.getFinalOrderNumber());
        verify(approvalRepository).save(approval);
    }

    @Test
    void markFailed_givenApprovedApproval_recordsFailureReason() {
        AgentPurchaseApproval approval = approval("approval-123", AgentPurchaseApprovalStatus.APPROVED);
        when(approvalRepository.findByApprovalIdForUpdate("approval-123")).thenReturn(Optional.of(approval));

        approvalService.markFailed("approval-123", "Payment failed");

        assertEquals(AgentPurchaseApprovalStatus.FAILED, approval.getStatus());
        assertEquals("Payment failed", approval.getFailureReason());
        verify(approvalRepository).save(approval);
    }

    @Test
    void listForUser_givenApprovals_returnsDtos() {
        when(approvalRepository.findByUserEmailOrderByCreatedAtDesc(USER_EMAIL))
                .thenReturn(List.of(approval("approval-123", AgentPurchaseApprovalStatus.PROPOSED)));

        List<AgentPurchaseApprovalDto> dtos = approvalService.listForUser(USER_EMAIL);

        assertEquals(1, dtos.size());
        assertEquals("approval-123", dtos.getFirst().approvalId());
    }

    private CreateAgentPurchaseApprovalRequest createRequest() {
        return new CreateAgentPurchaseApprovalRequest(
                USER_EMAIL,
                AGENT_ID,
                MANDATE_ID,
                "{\"listingId\":10}",
                "Best value",
                new BigDecimal("50.00"),
                new BigDecimal("5.00"),
                new BigDecimal("55.00"),
                "{\"policyId\":\"default\"}",
                null
        );
    }

    private AgentPurchaseApproval approval(String approvalId, AgentPurchaseApprovalStatus status) {
        AgentPurchaseApproval approval = new AgentPurchaseApproval();
        approval.setApprovalId(approvalId);
        approval.setUserEmail(USER_EMAIL);
        approval.setAgentId(AGENT_ID);
        approval.setMandateId(MANDATE_ID);
        approval.setStatus(status);
        approval.setProposedOrderSnapshot("{\"listingId\":10}");
        approval.setSubtotal(new BigDecimal("50.00"));
        approval.setServiceFee(new BigDecimal("5.00"));
        approval.setTotal(new BigDecimal("55.00"));
        approval.setProposedAt(Instant.now());
        return approval;
    }

    private Order order(BigDecimal total) {
        Order order = new Order();
        order.setTotal(total);
        return order;
    }
}
