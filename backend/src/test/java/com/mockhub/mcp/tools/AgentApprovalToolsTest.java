package com.mockhub.mcp.tools;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mockhub.agentapproval.dto.AgentPurchaseApprovalDto;
import com.mockhub.agentapproval.dto.CreateAgentPurchaseApprovalRequest;
import com.mockhub.agentapproval.service.AgentPurchaseApprovalService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AgentApprovalToolsTest {

    @Mock
    private AgentPurchaseApprovalService approvalService;

    private AgentApprovalTools tools;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
        tools = new AgentApprovalTools(approvalService, objectMapper);
    }

    @Test
    void proposePurchase_givenValidInput_createsProposal() {
        when(approvalService.createProposal(any())).thenReturn(dto("PROPOSED"));

        String result = tools.proposePurchase(
                "buyer@example.com",
                "agent-1",
                "mandate-1",
                "{\"listingId\":10}",
                "Best value",
                new BigDecimal("50.00"),
                new BigDecimal("5.00"),
                new BigDecimal("55.00"),
                "{\"policyId\":\"default\"}",
                "2026-05-07T12:00:00Z"
        );

        assertTrue(result.contains("\"approvalId\":\"approval-123\""));
        ArgumentCaptor<CreateAgentPurchaseApprovalRequest> captor =
                ArgumentCaptor.forClass(CreateAgentPurchaseApprovalRequest.class);
        verify(approvalService).createProposal(captor.capture());
        assertEquals("buyer@example.com", captor.getValue().userEmail());
        assertEquals(Instant.parse("2026-05-07T12:00:00Z"), captor.getValue().expiresAt());
    }

    @Test
    void proposePurchase_givenMissingAgentId_returnsErrorJson() {
        String result = tools.proposePurchase(
                "buyer@example.com",
                " ",
                "mandate-1",
                "{\"listingId\":10}",
                null,
                BigDecimal.ONE,
                BigDecimal.ZERO,
                BigDecimal.ONE,
                null,
                null
        );

        assertTrue(result.contains("\"error\""));
        assertTrue(result.contains("Agent ID is required"));
    }

    @Test
    void listPurchaseApprovals_givenUserEmail_returnsApprovalsJson() {
        when(approvalService.listForUser("buyer@example.com")).thenReturn(List.of(dto("PROPOSED")));

        String result = tools.listPurchaseApprovals("buyer@example.com");

        assertTrue(result.contains("\"approvalId\":\"approval-123\""));
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
