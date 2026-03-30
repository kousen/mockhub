package com.mockhub.ai.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record RecommendationDto(
        Long eventId,
        String eventName,
        String eventSlug,
        String venueName,
        String city,
        Instant eventDate,
        BigDecimal minPrice,
        double relevanceScore,
        String reason,
        boolean spotifyMatch
) {
}
