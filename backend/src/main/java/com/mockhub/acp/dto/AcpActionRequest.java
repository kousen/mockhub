package com.mockhub.acp.dto;

import jakarta.validation.constraints.NotBlank;

public record AcpActionRequest(
        @NotBlank(message = "Agent ID is required")
        String agentId,
        @NotBlank(message = "Mandate ID is required")
        String mandateId
) {
}
