package com.mockhub.acp.dto;

import jakarta.validation.constraints.NotBlank;

public record AcpCompleteRequest(
        @NotBlank(message = "Agent ID is required")
        String agentId,
        @NotBlank(message = "Mandate ID is required")
        String mandateId,
        String paymentIntentId
) {
}
