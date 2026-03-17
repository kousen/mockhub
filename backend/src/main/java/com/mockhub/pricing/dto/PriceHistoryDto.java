package com.mockhub.pricing.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record PriceHistoryDto(
        Long id,
        Long eventId,
        BigDecimal price,
        BigDecimal multiplier,
        BigDecimal supplyRatio,
        BigDecimal demandScore,
        int daysToEvent,
        Instant recordedAt
) {
}
