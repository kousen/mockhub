package com.mockhub.ticket.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record SaleDto(
        String orderNumber,
        String eventName,
        String sectionName,
        String seatInfo,
        BigDecimal pricePaid,
        Instant soldAt
) {
}
