package com.mockhub.mcp.tools;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mockhub.agentrisk.dto.AgentRiskSummaryDto;
import com.mockhub.agentrisk.service.AgentRiskService;
import com.mockhub.ai.service.ChatContext;

@Component
public class AgentRiskTools {

    private static final Logger log = LoggerFactory.getLogger(AgentRiskTools.class);

    private final AgentRiskService agentRiskService;
    private final ObjectMapper objectMapper;

    public AgentRiskTools(AgentRiskService agentRiskService, ObjectMapper objectMapper) {
        this.agentRiskService = agentRiskService;
        this.objectMapper = objectMapper;
    }

    @Tool(description = "Get a deterministic local risk summary for a MockHub agent and user. "
            + "Returns recent risk signals, warning reasons, and whether the agent is blocked by risk thresholds.")
    public String getAgentRiskSummary(
            @ToolParam(description = "Email of the user whose agent risk summary should be queried", required = true)
                    String userEmail,
            @ToolParam(description = "ID of the agent to summarize", required = true)
                    String agentId) {
        try {
            String effectiveEmail = ChatContext.resolveEmail(userEmail);
            AgentRiskSummaryDto summary = agentRiskService.summarizeRisk(effectiveEmail, agentId);
            return objectMapper.writeValueAsString(summary);
        } catch (Exception e) {
            log.error("Error getting agent risk summary for agent '{}' and user '{}': {}",
                    agentId, userEmail, e.getMessage(), e);
            return objectMapper.createObjectNode()
                    .put("error", "Failed to get agent risk summary: " + e.getMessage())
                    .toString();
        }
    }
}
