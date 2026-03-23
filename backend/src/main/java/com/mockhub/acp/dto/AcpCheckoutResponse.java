package com.mockhub.acp.dto;

import java.time.Instant;
import java.util.List;

public record AcpCheckoutResponse(
        String checkoutId,
        String status,
        String buyerEmail,
        List<AcpLineItemResponse> lineItems,
        AcpPricing pricing,
        Instant createdAt,
        Instant completedAt
) {
}
