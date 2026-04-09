package com.mockhub.order.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record OrderSummaryDto(
        Long id,
        String orderNumber,
        String status,
        BigDecimal total,
        int itemCount,
        Instant createdAt,
        String eventName,
        Instant eventDate,
        String venueName,
        String agentId
) {
}
