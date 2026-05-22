package com.mockhub.mcp.tools;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mockhub.agentpurchaseevidence.dto.AgentPurchaseEvidenceDto;
import com.mockhub.agentpurchaseevidence.dto.AgentPurchaseEvidenceDto.AgentPurchaseEvidenceFulfillmentDto;
import com.mockhub.agentpurchaseevidence.dto.AgentPurchaseEvidenceDto.AgentPurchaseEvidenceTicketArtifactDto;
import com.mockhub.agentpurchaseevidence.service.AgentPurchaseEvidenceService;
import com.mockhub.ai.service.ChatContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AgentPurchaseEvidenceToolsTest {

    private static final String ORDER_NUMBER = "MH-20260522-0001";

    @Mock
    private AgentPurchaseEvidenceService evidenceService;

    private AgentPurchaseEvidenceTools tools;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
        tools = new AgentPurchaseEvidenceTools(evidenceService, objectMapper);
    }

    @AfterEach
    void tearDown() {
        ChatContext.clear();
    }

    @Test
    @DisplayName("getAgentPurchaseEvidence - valid request - returns evidence JSON")
    void getAgentPurchaseEvidence_givenValidRequest_returnsEvidenceJson() {
        when(evidenceService.getEvidenceForUser("buyer@example.com", ORDER_NUMBER))
                .thenReturn(evidenceDto("buyer@example.com"));

        String result = tools.getAgentPurchaseEvidence("buyer@example.com", ORDER_NUMBER);

        assertThat(result).contains("\"orderNumber\":\"" + ORDER_NUMBER + "\"");
        assertThat(result).contains("\"agentId\":\"agent-1\"");
        assertThat(result).contains("\"ticketPdfUrl\":\"/api/v1/orders/" + ORDER_NUMBER);
        assertThat(result).contains("\"publicTicketViewUrl\":null");
        assertThat(result).contains("\"qrCodeUrl\":null");
        assertThat(result).doesNotContain("order-view-token");
        verify(evidenceService).getEvidenceForUser("buyer@example.com", ORDER_NUMBER);
    }

    @Test
    @DisplayName("getAgentPurchaseEvidence - ChatContext overrides userEmail")
    void getAgentPurchaseEvidence_givenChatContext_usesAuthenticatedEmail() {
        ChatContext.setAuthenticatedEmail("real@example.com");
        when(evidenceService.getEvidenceForUser("real@example.com", ORDER_NUMBER))
                .thenReturn(evidenceDto("real@example.com"));

        tools.getAgentPurchaseEvidence("attacker@example.com", ORDER_NUMBER);

        verify(evidenceService).getEvidenceForUser("real@example.com", ORDER_NUMBER);
    }

    @Test
    @DisplayName("getAgentPurchaseEvidence - service failure - returns error JSON")
    void getAgentPurchaseEvidence_givenServiceFailure_returnsErrorJson() {
        when(evidenceService.getEvidenceForUser("buyer@example.com", ORDER_NUMBER))
                .thenThrow(new IllegalArgumentException("Order number is required"));

        String result = tools.getAgentPurchaseEvidence("buyer@example.com", ORDER_NUMBER);

        assertThat(result).contains("\"error\"");
        assertThat(result).contains("Failed to get agent purchase evidence");
    }

    private AgentPurchaseEvidenceDto evidenceDto(String userEmail) {
        return new AgentPurchaseEvidenceDto(
                ORDER_NUMBER,
                userEmail,
                "agent-1",
                "CONFIRMED",
                Instant.now(),
                null,
                null,
                null,
                null,
                null,
                new AgentPurchaseEvidenceFulfillmentDto(
                        true,
                        "https://mockhub.example/tickets/view?token=order-view-token",
                        "order-view",
                        "mockhub.ticket.signing-secret",
                        null,
                        null,
                        List.of(new AgentPurchaseEvidenceTicketArtifactDto(
                                10L,
                                20L,
                                "/api/v1/orders/" + ORDER_NUMBER + "/tickets/10/download",
                                "/api/v1/tickets/" + ORDER_NUMBER + "/10/qr?token=order-view-token",
                                "ticket-verification",
                                null))),
                List.of(),
                List.of(),
                List.of());
    }
}
