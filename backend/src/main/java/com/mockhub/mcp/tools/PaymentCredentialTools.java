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
import com.mockhub.ai.service.ChatContext;
import com.mockhub.paymentcredential.dto.CreatePaymentCredentialRequest;
import com.mockhub.paymentcredential.dto.PaymentCredentialDto;
import com.mockhub.paymentcredential.service.PaymentCredentialService;

@Component
public class PaymentCredentialTools {

    private static final Logger log = LoggerFactory.getLogger(PaymentCredentialTools.class);

    private final PaymentCredentialService paymentCredentialService;
    private final ObjectMapper objectMapper;

    public PaymentCredentialTools(PaymentCredentialService paymentCredentialService,
                                  ObjectMapper objectMapper) {
        this.paymentCredentialService = paymentCredentialService;
        this.objectMapper = objectMapper;
    }

    @Tool(description = "Issue a scoped MockHub payment credential for an agent. "
            + "Credentials authorize payment with a specific mock-backed instrument, amount limit, "
            + "currency, usage type, and optional expiration. They are distinct from mandates.")
    public String issuePaymentCredential(
            @ToolParam(description = "Email of the user granting payment authority", required = true)
                    String userEmail,
            @ToolParam(description = "ID of the agent receiving payment authority", required = true)
                    String agentId,
            @ToolParam(description = "Maximum order total this credential can pay", required = true)
                    BigDecimal maxAmount,
            @ToolParam(description = "Currency, currently USD only (optional, defaults to USD)", required = false)
                    String currency,
            @ToolParam(description = "ONE_TIME or REUSABLE (optional, defaults to ONE_TIME)", required = false)
                    String usage,
            @ToolParam(description = "Backing payment method, currently mock only (optional, defaults to mock)",
                    required = false)
                    String backingPaymentMethod,
            @ToolParam(description = "ISO-8601 expiration timestamp (optional, null = no expiration)",
                    required = false)
                    String expiresAt) {
        try {
            String effectiveEmail = ChatContext.resolveEmail(userEmail);
            Instant parsedExpiresAt = expiresAt != null && !expiresAt.isBlank()
                    ? Instant.parse(expiresAt.strip())
                    : null;
            PaymentCredentialDto credential = paymentCredentialService.issueCredential(
                    new CreatePaymentCredentialRequest(
                            effectiveEmail,
                            agentId,
                            maxAmount,
                            currency,
                            usage,
                            backingPaymentMethod,
                            parsedExpiresAt));
            return objectMapper.writeValueAsString(credential);
        } catch (Exception e) {
            log.error("Error issuing payment credential for agent '{}' and user '{}': {}",
                    agentId, userEmail, e.getMessage(), e);
            return errorJson("Failed to issue payment credential: " + e.getMessage());
        }
    }

    @Tool(description = "List all MockHub payment credentials for a user, including active, consumed, revoked, "
            + "and expired records.")
    public String listPaymentCredentials(
            @ToolParam(description = "Email of the user whose payment credentials to list", required = true)
                    String userEmail) {
        try {
            String effectiveEmail = ChatContext.resolveEmail(userEmail);
            List<PaymentCredentialDto> credentials = paymentCredentialService.listCredentials(effectiveEmail);
            return objectMapper.writeValueAsString(credentials);
        } catch (Exception e) {
            log.error("Error listing payment credentials for '{}': {}", userEmail, e.getMessage(), e);
            return errorJson("Failed to list payment credentials: " + e.getMessage());
        }
    }

    @Tool(description = "Revoke an active MockHub payment credential. Revoked credentials can no longer pay.")
    public String revokePaymentCredential(
            @ToolParam(description = "Email of the user who owns the payment credential", required = true)
                    String userEmail,
            @ToolParam(description = "Payment credential ID to revoke", required = true)
                    String credentialId) {
        try {
            String effectiveEmail = ChatContext.resolveEmail(userEmail);
            PaymentCredentialDto credential = paymentCredentialService.revokeCredential(
                    credentialId, effectiveEmail);
            return objectMapper.writeValueAsString(credential);
        } catch (Exception e) {
            log.error("Error revoking payment credential '{}' for '{}': {}",
                    credentialId, userEmail, e.getMessage(), e);
            return errorJson("Failed to revoke payment credential: " + e.getMessage());
        }
    }

    private String errorJson(String message) {
        return objectMapper.createObjectNode()
                .put("error", message)
                .toString();
    }
}
