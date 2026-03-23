package com.mockhub.acp.dto;

import java.math.BigDecimal;

public record AcpLineItemResponse(
        Long listingId,
        String eventName,
        String eventSlug,
        String section,
        String row,
        String seat,
        BigDecimal unitPrice,
        int quantity
) {
}
