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
import com.mockhub.agentrisk.dto.AgentRiskSummaryDto;
import com.mockhub.agentrisk.service.AgentRiskService;
import com.mockhub.ai.service.ChatContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AgentRiskToolsTest {

    @Mock
    private AgentRiskService agentRiskService;

    private AgentRiskTools agentRiskTools;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
        agentRiskTools = new AgentRiskTools(agentRiskService, objectMapper);
    }

    @AfterEach
    void tearDown() {
        ChatContext.clear();
    }

    @Test
    @DisplayName("getAgentRiskSummary - valid request - returns summary JSON")
    void getAgentRiskSummary_givenValidRequest_returnsSummaryJson() {
        when(agentRiskService.summarizeRisk("buyer@example.com", "agent-1"))
                .thenReturn(summary(false, List.of("High spend attempt signals: 1")));

        String result = agentRiskTools.getAgentRiskSummary("buyer@example.com", "agent-1");

        assertThat(result).contains("\"agentId\":\"agent-1\"");
        assertThat(result).contains("High spend attempt");
    }

    @Test
    @DisplayName("getAgentRiskSummary - ChatContext overrides user email")
    void getAgentRiskSummary_givenChatContext_usesAuthenticatedEmail() {
        ChatContext.setAuthenticatedEmail("real@example.com");
        when(agentRiskService.summarizeRisk("real@example.com", "agent-1"))
                .thenReturn(summary(false, List.of()));

        agentRiskTools.getAgentRiskSummary("attacker@example.com", "agent-1");

        verify(agentRiskService).summarizeRisk("real@example.com", "agent-1");
    }

    @Test
    @DisplayName("getAgentRiskSummary - service failure - returns error JSON")
    void getAgentRiskSummary_givenServiceFailure_returnsErrorJson() {
        when(agentRiskService.summarizeRisk("buyer@example.com", "agent-1"))
                .thenThrow(new IllegalArgumentException("Agent ID is required"));

        String result = agentRiskTools.getAgentRiskSummary("buyer@example.com", "agent-1");

        assertThat(result).contains("\"error\"");
        assertThat(result).contains("Failed to get agent risk summary");
    }

    private AgentRiskSummaryDto summary(boolean blocked, List<String> reasons) {
        return new AgentRiskSummaryDto(
                "buyer@example.com",
                "agent-1",
                Instant.now().minusSeconds(3600),
                reasons.size(),
                reasons.size(),
                blocked ? 1 : 0,
                blocked ? "CRITICAL" : "WARNING",
                blocked,
                reasons,
                List.of());
    }
}
