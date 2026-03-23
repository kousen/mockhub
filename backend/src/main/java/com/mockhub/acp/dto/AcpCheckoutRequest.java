package com.mockhub.acp.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AcpCheckoutRequest(
        @NotBlank(message = "Buyer email is required")
        String buyerEmail,
        @NotNull(message = "Line items are required")
        @Valid
        List<AcpLineItem> lineItems,
        String paymentMethod,
        String idempotencyKey,
        String mandateId
) {
}
