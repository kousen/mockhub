package com.mockhub.agentapproval.controller;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.mockhub.agentapproval.dto.AgentPurchaseApprovalDto;
import com.mockhub.agentapproval.dto.CreateAgentPurchaseApprovalRequest;
import com.mockhub.agentapproval.dto.DenyAgentPurchaseApprovalRequest;
import com.mockhub.agentapproval.dto.WebCreateAgentPurchaseApprovalRequest;
import com.mockhub.agentapproval.service.AgentPurchaseApprovalService;
import com.mockhub.auth.entity.User;
import com.mockhub.auth.security.SecurityUser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AgentPurchaseApprovalControllerTest {

    @Mock
    private AgentPurchaseApprovalService approvalService;

    private AgentPurchaseApprovalController controller;
    private SecurityUser securityUser;

    @BeforeEach
    void setUp() {
        controller = new AgentPurchaseApprovalController(approvalService);
        User user = new User();
        user.setId(1L);
        user.setEmail("buyer@example.com");
        user.setPasswordHash("hash");
        securityUser = new SecurityUser(user);
    }

    @Test
    void createProposal_givenAuthenticatedUser_usesPrincipalEmail() {
        when(approvalService.createProposal(any())).thenReturn(dto("PROPOSED"));
        WebCreateAgentPurchaseApprovalRequest request = new WebCreateAgentPurchaseApprovalRequest(
                "agent-1",
                "mandate-1",
                "{\"listingId\":10}",
                "Best value",
                new BigDecimal("50.00"),
                new BigDecimal("5.00"),
                new BigDecimal("55.00"),
                "{\"policyId\":\"default\"}",
                null
        );

        ResponseEntity<AgentPurchaseApprovalDto> response =
                controller.createProposal(securityUser, request);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        verify(approvalService).createProposal(any(CreateAgentPurchaseApprovalRequest.class));
    }

    @Test
    void listApprovals_givenAuthenticatedUser_returnsUserApprovals() {
        when(approvalService.listForUser("buyer@example.com")).thenReturn(List.of(dto("PROPOSED")));

        ResponseEntity<List<AgentPurchaseApprovalDto>> response = controller.listApprovals(securityUser);

        assertEquals(1, response.getBody().size());
    }

    @Test
    void approve_givenApprovalId_delegatesToService() {
        when(approvalService.approve("approval-123", "buyer@example.com")).thenReturn(dto("APPROVED"));

        ResponseEntity<AgentPurchaseApprovalDto> response = controller.approve(securityUser, "approval-123");

        assertEquals("APPROVED", response.getBody().status());
    }

    @Test
    void deny_givenReason_delegatesToService() {
        when(approvalService.deny(eq("approval-123"), eq("buyer@example.com"), eq("Too expensive")))
                .thenReturn(dto("DENIED"));

        ResponseEntity<AgentPurchaseApprovalDto> response = controller.deny(
                securityUser, "approval-123", new DenyAgentPurchaseApprovalRequest("Too expensive"));

        assertEquals("DENIED", response.getBody().status());
    }

    private AgentPurchaseApprovalDto dto(String status) {
        return new AgentPurchaseApprovalDto(
                "approval-123",
                "buyer@example.com",
                "agent-1",
                "mandate-1",
                status,
                "{\"listingId\":10}",
                "Best value",
                new BigDecimal("50.00"),
                new BigDecimal("5.00"),
                new BigDecimal("55.00"),
                "{\"policyId\":\"default\"}",
                Instant.now(),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                Instant.now()
        );
    }
}
