package com.mockhub.acp.dto;

import java.time.Instant;
import java.util.List;

import com.mockhub.commerce.dto.CommercePolicyDto;

public record AcpCheckoutResponse(
        String checkoutId,
        String status,
        String buyerEmail,
        List<AcpLineItemResponse> lineItems,
        AcpPricing pricing,
        CommercePolicyDto commercePolicy,
        Instant createdAt,
        Instant completedAt
) {
}
