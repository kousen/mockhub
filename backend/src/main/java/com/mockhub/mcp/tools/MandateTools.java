package com.mockhub.mcp.tools;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mockhub.mandate.dto.CreateMandateRequest;
import com.mockhub.mandate.dto.MandateDto;
import com.mockhub.mandate.service.MandateService;

@Component
public class MandateTools {

    private static final Logger log = LoggerFactory.getLogger(MandateTools.class);

    private final MandateService mandateService;
    private final ObjectMapper objectMapper;

    public MandateTools(MandateService mandateService, ObjectMapper objectMapper) {
        this.mandateService = mandateService;
        this.objectMapper = objectMapper;
    }

    @Tool(description = "Create a new mandate granting an agent permission to act on behalf of a user. "
            + "Scope can be BROWSE (read-only) or PURCHASE (can buy tickets). "
            + "Spending limits and category/event restrictions are optional.")
    @SuppressWarnings("java:S107") // MCP tool methods require separate params for each tool argument
    public String createMandate(
            @ToolParam(description = "ID of the agent being granted the mandate", required = true) String agentId,
            @ToolParam(description = "Email of the user granting the mandate", required = true) String userEmail,
            @ToolParam(description = "Scope of the mandate: BROWSE or PURCHASE", required = true) String scope,
            @ToolParam(description = "Maximum spend per transaction (optional)") BigDecimal maxSpendPerTransaction,
            @ToolParam(description = "Maximum cumulative spend limit (optional)") BigDecimal maxSpendTotal,
            @ToolParam(description = "Comma-separated category slugs (optional, null = all)") String allowedCategories,
            @ToolParam(description = "Comma-separated event slugs (optional, null = all)") String allowedEvents,
            @ToolParam(description = "ISO-8601 expiration timestamp (optional, null = no expiration)") String expiresAt) {
        try {
            Instant parsedExpiresAt = expiresAt != null ? Instant.parse(expiresAt) : null;
            CreateMandateRequest request = new CreateMandateRequest(
                    agentId, userEmail, scope,
                    maxSpendPerTransaction, maxSpendTotal,
                    allowedCategories, allowedEvents,
                    parsedExpiresAt);
            MandateDto mandate = mandateService.createMandate(request);
            return objectMapper.writeValueAsString(mandate);
        } catch (Exception e) {
            log.error("Error creating mandate for agent '{}' and user '{}': {}",
                    agentId, userEmail, e.getMessage(), e);
            return errorJson("Failed to create mandate: " + e.getMessage());
        }
    }

    @Tool(description = "Revoke an existing mandate by its mandate ID. "
            + "The mandate will be marked as REVOKED and can no longer be used.")
    public String revokeMandate(
            @ToolParam(description = "The unique mandate ID (UUID) to revoke", required = true) String mandateId) {
        try {
            mandateService.revokeMandate(mandateId);
            return "{\"status\": \"success\", \"message\": \"Mandate " + mandateId + " revoked\"}";
        } catch (Exception e) {
            log.error("Error revoking mandate '{}': {}", mandateId, e.getMessage(), e);
            return errorJson("Failed to revoke mandate: " + e.getMessage());
        }
    }

    @Tool(description = "List all active mandates for a user by their email address.")
    public String listMandates(
            @ToolParam(description = "Email of the user whose mandates to list", required = true) String userEmail) {
        try {
            List<MandateDto> mandates = mandateService.listMandates(userEmail);
            return objectMapper.writeValueAsString(mandates);
        } catch (Exception e) {
            log.error("Error listing mandates for '{}': {}", userEmail, e.getMessage(), e);
            return errorJson("Failed to list mandates: " + e.getMessage());
        }
    }

    @Tool(description = "Validate whether an agent has an active mandate to perform an action for a user. "
            + "Returns whether the action is authorized based on scope, spending limits, "
            + "and optional category/event restrictions. Always pass eventSlug and categorySlug "
            + "when validating a purchase to catch event-specific restrictions early.")
    public String validateMandate(
            @ToolParam(description = "ID of the agent requesting authorization", required = true) String agentId,
            @ToolParam(description = "Email of the user the agent is acting for", required = true) String userEmail,
            @ToolParam(description = "Required scope: BROWSE or PURCHASE", required = true) String scope,
            @ToolParam(description = "Transaction amount to validate against spending limits (optional)") BigDecimal amount,
            @ToolParam(description = "Category slug to check against allowed categories (optional)") String categorySlug,
            @ToolParam(description = "Event slug to check against allowed events (optional)") String eventSlug) {
        try {
            boolean authorized = mandateService.validateAction(
                    agentId, userEmail, scope, amount, categorySlug, eventSlug);
            if (authorized) {
                return "{\"authorized\": true, \"message\": \"Action is authorized\"}";
            } else {
                return "{\"authorized\": false, \"message\": \"Action is not authorized by any active mandate\"}";
            }
        } catch (Exception e) {
            log.error("Error validating mandate for agent '{}' and user '{}': {}",
                    agentId, userEmail, e.getMessage(), e);
            return errorJson("Failed to validate mandate: " + e.getMessage());
        }
    }

    @Tool(description = "RECOMMENDED before addToCart — finds the best matching mandate for a specific "
            + "purchase action. Checks scope, spending limits, category, and event restrictions in one call. "
            + "Returns the matching mandate or an error explaining why none match.")
    public String getBestMandate(
            @ToolParam(description = "ID of the agent requesting authorization", required = true) String agentId,
            @ToolParam(description = "Email of the user the agent is acting for", required = true) String userEmail,
            @ToolParam(description = "Event slug to check against allowed events", required = true) String eventSlug,
            @ToolParam(description = "Category slug to check against allowed categories (optional)") String categorySlug,
            @ToolParam(description = "Transaction amount to validate against spending limits (optional)") BigDecimal amount) {
        try {
            Optional<MandateDto> mandate = mandateService.findBestMandate(
                    agentId, userEmail, "PURCHASE", amount, categorySlug, eventSlug);
            if (mandate.isPresent()) {
                return objectMapper.writeValueAsString(mandate.get());
            }
            return "{\"error\": \"No active mandate found that authorizes this purchase. "
                    + "Check scope, spending limits, and event/category restrictions.\"}";
        } catch (Exception e) {
            log.error("Error finding best mandate for agent '{}' and user '{}': {}",
                    agentId, userEmail, e.getMessage(), e);
            return errorJson("Failed to find best mandate: " + e.getMessage());
        }
    }

    private String errorJson(String message) {
        return "{\"error\": \"" + message.replace("\"", "'") + "\"}";
    }
}
