package com.mockhub.mcp.tools;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mockhub.agentpurchaseevidence.dto.AgentPurchaseEvidenceDto;
import com.mockhub.agentpurchaseevidence.dto.AgentPurchaseEvidenceDto.AgentPurchaseEvidenceFulfillmentDto;
import com.mockhub.agentpurchaseevidence.dto.AgentPurchaseEvidenceDto.AgentPurchaseEvidenceTicketArtifactDto;
import com.mockhub.agentpurchaseevidence.service.AgentPurchaseEvidenceService;
import com.mockhub.ai.service.ChatContext;

@Component
public class AgentPurchaseEvidenceTools {

    private static final Logger log = LoggerFactory.getLogger(AgentPurchaseEvidenceTools.class);

    private final AgentPurchaseEvidenceService evidenceService;
    private final ObjectMapper objectMapper;

    public AgentPurchaseEvidenceTools(AgentPurchaseEvidenceService evidenceService,
                                      ObjectMapper objectMapper) {
        this.evidenceService = evidenceService;
        this.objectMapper = objectMapper;
    }

    @Tool(description = "Get the read-only evidence trail for a MockHub agent-initiated order. "
            + "Returns mandate, purchase approval, scoped payment credential, checkout, risk, eval, "
            + "and fulfillment evidence for the order owner.")
    public String getAgentPurchaseEvidence(
            @ToolParam(description = "Email of the user who owns the order", required = true)
                    String userEmail,
            @ToolParam(description = "Order number to inspect (e.g. 'MH-20260319-0001')", required = true)
                    String orderNumber) {
        try {
            String effectiveEmail = ChatContext.resolveEmail(userEmail);
            AgentPurchaseEvidenceDto evidence = evidenceService.getEvidenceForUser(effectiveEmail, orderNumber);
            return objectMapper.writeValueAsString(redactPublicTicketTokens(evidence));
        } catch (Exception e) {
            log.error("Error getting agent purchase evidence for order '{}' and user '{}': {}",
                    orderNumber, userEmail, e.getMessage(), e);
            return objectMapper.createObjectNode()
                    .put("error", "Failed to get agent purchase evidence: " + e.getMessage())
                    .toString();
        }
    }

    private AgentPurchaseEvidenceDto redactPublicTicketTokens(AgentPurchaseEvidenceDto evidence) {
        AgentPurchaseEvidenceFulfillmentDto fulfillment = evidence.fulfillment();
        if (fulfillment == null) {
            return evidence;
        }

        var redactedArtifacts = fulfillment.ticketArtifacts() == null
                ? null
                : fulfillment.ticketArtifacts().stream()
                        .map(artifact -> new AgentPurchaseEvidenceTicketArtifactDto(
                                artifact.ticketId(),
                                artifact.orderItemId(),
                                artifact.ticketPdfUrl(),
                                null,
                                artifact.verificationTokenType(),
                                artifact.scannedAt()))
                        .toList();

        AgentPurchaseEvidenceFulfillmentDto redactedFulfillment = new AgentPurchaseEvidenceFulfillmentDto(
                fulfillment.ticketPdfAvailable(),
                null,
                fulfillment.orderViewTokenType(),
                fulfillment.qrSigningKeyReference(),
                fulfillment.emailDispatch(),
                fulfillment.smsDispatch(),
                redactedArtifacts);

        return new AgentPurchaseEvidenceDto(
                evidence.orderNumber(),
                evidence.userEmail(),
                evidence.agentId(),
                evidence.status(),
                evidence.createdAt(),
                evidence.order(),
                evidence.mandate(),
                evidence.approval(),
                evidence.paymentCredential(),
                evidence.checkout(),
                redactedFulfillment,
                evidence.riskSignals(),
                evidence.evalOutcomes(),
                evidence.actorTimeline());
    }
}
