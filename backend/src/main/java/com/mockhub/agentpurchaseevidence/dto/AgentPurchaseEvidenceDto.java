package com.mockhub.agentpurchaseevidence.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record AgentPurchaseEvidenceDto(
        String orderNumber,
        String userEmail,
        String agentId,
        String status,
        Instant createdAt,
        AgentPurchaseEvidenceOrderDto order,
        AgentPurchaseEvidenceMandateDto mandate,
        AgentPurchaseEvidenceApprovalDto approval,
        AgentPurchaseEvidencePaymentCredentialDto paymentCredential,
        AgentPurchaseEvidenceCheckoutDto checkout,
        AgentPurchaseEvidenceFulfillmentDto fulfillment,
        List<AgentPurchaseEvidenceRiskSignalDto> riskSignals,
        List<AgentPurchaseEvidenceEvalOutcomeDto> evalOutcomes,
        List<AgentPurchaseEvidenceActorStepDto> actorTimeline
) {

    public record AgentPurchaseEvidenceOrderDto(
            Long id,
            String orderNumber,
            String status,
            BigDecimal subtotal,
            BigDecimal serviceFee,
            BigDecimal total,
            String paymentMethod,
            String paymentIntentId,
            String agentId,
            String mandateId,
            Instant confirmedAt,
            Instant createdAt,
            List<AgentPurchaseEvidenceOrderItemDto> items
    ) {
    }

    public record AgentPurchaseEvidenceOrderItemDto(
            Long id,
            Long listingId,
            Long ticketId,
            String eventName,
            String eventSlug,
            Instant eventDate,
            String venueName,
            String sectionName,
            String rowLabel,
            String seatNumber,
            String ticketType,
            BigDecimal pricePaid,
            Instant scannedAt
    ) {
    }

    public record AgentPurchaseEvidenceMandateDto(
            String mandateId,
            String agentId,
            String userEmail,
            String scope,
            BigDecimal maxSpendPerTransaction,
            BigDecimal maxSpendTotal,
            BigDecimal totalSpent,
            BigDecimal remainingBudget,
            String allowedCategories,
            String allowedEvents,
            String allowedSections,
            String approvalMode,
            String status,
            Instant expiresAt,
            Instant revokedAt,
            Instant createdAt
    ) {
    }

    public record AgentPurchaseEvidenceApprovalDto(
            String approvalId,
            String userEmail,
            String agentId,
            String mandateId,
            String status,
            String proposedOrderSnapshot,
            String agentRationale,
            BigDecimal subtotal,
            BigDecimal serviceFee,
            BigDecimal total,
            String commercePolicySnapshot,
            Instant proposedAt,
            Instant approvedAt,
            Instant deniedAt,
            Instant expiresAt,
            Instant completedAt,
            Instant failedAt,
            String finalOrderNumber,
            String denialReason,
            String failureReason,
            Instant createdAt
    ) {
    }

    public record AgentPurchaseEvidencePaymentCredentialDto(
            String credentialId,
            String userEmail,
            String agentId,
            String allowedMerchant,
            BigDecimal maxAmount,
            String currency,
            String usage,
            String status,
            String backingPaymentMethod,
            Instant expiresAt,
            Instant revokedAt,
            Instant consumedAt,
            String consumedByOrderNumber,
            Instant lastUsedAt,
            Instant createdAt
    ) {
    }

    public record AgentPurchaseEvidenceCheckoutDto(
            String checkoutId,
            String acpStatus,
            String paymentMethod,
            String paymentIntentId,
            String idempotencyKey,
            boolean agentInitiated,
            Instant createdAt,
            Instant completedAt
    ) {
    }

    public record AgentPurchaseEvidenceFulfillmentDto(
            boolean ticketPdfAvailable,
            String publicTicketViewUrl,
            String orderViewTokenType,
            String qrSigningKeyReference,
            AgentPurchaseEvidenceDispatchDto emailDispatch,
            AgentPurchaseEvidenceDispatchDto smsDispatch,
            List<AgentPurchaseEvidenceTicketArtifactDto> ticketArtifacts
    ) {
    }

    public record AgentPurchaseEvidenceDispatchDto(
            String channel,
            String status,
            String recipient,
            Instant attemptedAt,
            String providerReference,
            String note
    ) {
    }

    public record AgentPurchaseEvidenceTicketArtifactDto(
            Long ticketId,
            Long orderItemId,
            String ticketPdfUrl,
            String qrCodeUrl,
            String verificationTokenType,
            Instant scannedAt
    ) {
    }

    public record AgentPurchaseEvidenceRiskSignalDto(
            Long id,
            String signalType,
            String severity,
            String actionType,
            String resourceType,
            String resourceId,
            String orderNumber,
            String mandateId,
            BigDecimal amount,
            String message,
            Instant createdAt
    ) {
    }

    public record AgentPurchaseEvidenceEvalOutcomeDto(
            String ruleName,
            String status,
            String severity,
            String message,
            Instant recordedAt,
            String sourceReference
    ) {
    }

    public record AgentPurchaseEvidenceActorStepDto(
            String step,
            String actorType,
            String actorId,
            Instant timestamp,
            String referenceId,
            String detail
    ) {
    }
}
