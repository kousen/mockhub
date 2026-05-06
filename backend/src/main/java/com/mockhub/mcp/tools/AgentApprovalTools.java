package com.mockhub.mcp.tools;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mockhub.agentapproval.dto.AgentPurchaseApprovalDto;
import com.mockhub.agentapproval.dto.CreateAgentPurchaseApprovalRequest;
import com.mockhub.agentapproval.service.AgentPurchaseApprovalService;
import com.mockhub.ai.service.ChatContext;

@Component
public class AgentApprovalTools {

    private static final Logger log = LoggerFactory.getLogger(AgentApprovalTools.class);

    private final AgentPurchaseApprovalService approvalService;
    private final ObjectMapper objectMapper;

    public AgentApprovalTools(AgentPurchaseApprovalService approvalService, ObjectMapper objectMapper) {
        this.approvalService = approvalService;
        this.objectMapper = objectMapper;
    }

    @Tool(description = "Create a durable MockHub audit record for an agent purchase proposal. "
            + "Use this before asking a user to approve or deny a proposed ticket purchase.")
    public String proposePurchase(
            @ToolParam(description = "Email of the user the agent is acting for", required = true) String userEmail,
            @ToolParam(description = "ID of the agent proposing the purchase", required = true) String agentId,
            @ToolParam(description = "Mandate ID authorizing this proposed purchase", required = true) String mandateId,
            @ToolParam(description = "JSON or text snapshot of proposed listings/order details",
                    required = true) String proposedOrderSnapshot,
            @ToolParam(description = "Agent's concise rationale for this purchase", required = false)
                    String agentRationale,
            @ToolParam(description = "Subtotal before fees", required = true) BigDecimal subtotal,
            @ToolParam(description = "Service fee amount", required = true) BigDecimal serviceFee,
            @ToolParam(description = "Total purchase amount", required = true) BigDecimal total,
            @ToolParam(description = "Optional commerce policy snapshot shown to the user", required = false)
                    String commercePolicySnapshot,
            @ToolParam(description = "Optional ISO-8601 expiration timestamp for the approval", required = false)
                    String expiresAt) {
        try {
            CreateAgentPurchaseApprovalRequest request = new CreateAgentPurchaseApprovalRequest(
                    ChatContext.resolveEmail(userEmail),
                    required(agentId, "Agent ID"),
                    required(mandateId, "Mandate ID"),
                    required(proposedOrderSnapshot, "Proposed order snapshot"),
                    agentRationale,
                    requireAmount(subtotal, "Subtotal"),
                    requireAmount(serviceFee, "Service fee"),
                    requireAmount(total, "Total"),
                    commercePolicySnapshot,
                    parseInstant(expiresAt)
            );
            AgentPurchaseApprovalDto approval = approvalService.createProposal(request);
            return objectMapper.writeValueAsString(approval);
        } catch (Exception e) {
            log.error("Error proposing agent purchase for '{}': {}", userEmail, e.getMessage(), e);
            return errorJson("Failed to propose purchase: " + e.getMessage());
        }
    }

    @Tool(description = "Approve a proposed MockHub agent purchase. "
            + "An approved ID can be supplied later to checkout completion for audit linkage.")
    public String approvePurchase(
            @ToolParam(description = "Email of the user approving the purchase", required = true) String userEmail,
            @ToolParam(description = "Purchase approval ID", required = true) String approvalId) {
        try {
            AgentPurchaseApprovalDto approval = approvalService.approve(
                    required(approvalId, "Approval ID"), ChatContext.resolveEmail(userEmail));
            return objectMapper.writeValueAsString(approval);
        } catch (Exception e) {
            log.error("Error approving purchase '{}': {}", approvalId, e.getMessage(), e);
            return errorJson("Failed to approve purchase: " + e.getMessage());
        }
    }

    @Tool(description = "Deny a proposed MockHub agent purchase and record the user's reason when provided.")
    public String denyPurchase(
            @ToolParam(description = "Email of the user denying the purchase", required = true) String userEmail,
            @ToolParam(description = "Purchase approval ID", required = true) String approvalId,
            @ToolParam(description = "Optional reason for denial", required = false) String reason) {
        try {
            AgentPurchaseApprovalDto approval = approvalService.deny(
                    required(approvalId, "Approval ID"), ChatContext.resolveEmail(userEmail), reason);
            return objectMapper.writeValueAsString(approval);
        } catch (Exception e) {
            log.error("Error denying purchase '{}': {}", approvalId, e.getMessage(), e);
            return errorJson("Failed to deny purchase: " + e.getMessage());
        }
    }

    @Tool(description = "List MockHub agent purchase proposals, approvals, denials, and outcomes for a user.")
    public String listPurchaseApprovals(
            @ToolParam(description = "Email of the user whose approvals should be listed", required = true)
                    String userEmail) {
        try {
            List<AgentPurchaseApprovalDto> approvals = approvalService.listForUser(ChatContext.resolveEmail(userEmail));
            return objectMapper.writeValueAsString(approvals);
        } catch (Exception e) {
            log.error("Error listing purchase approvals for '{}': {}", userEmail, e.getMessage(), e);
            return errorJson("Failed to list purchase approvals: " + e.getMessage());
        }
    }

    private String required(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.strip();
    }

    private BigDecimal requireAmount(BigDecimal amount, String fieldName) {
        if (amount == null) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return amount;
    }

    private Instant parseInstant(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return Instant.parse(value.strip());
    }

    private String errorJson(String message) {
        return "{\"error\": \"" + message.replace("\"", "'") + "\"}";
    }
}
