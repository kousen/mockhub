package com.mockhub.ai.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record PricePredictionDto(
        String eventSlug,
        BigDecimal currentPrice,
        BigDecimal predictedPrice,
        String trend,
        double confidence,
        Instant predictedAt
) {
}
