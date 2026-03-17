package com.mockhub.event.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record EventSummaryDto(
        Long id,
        String name,
        String slug,
        String artistName,
        String venueName,
        String city,
        Instant eventDate,
        BigDecimal minPrice,
        int availableTickets,
        String primaryImageUrl,
        String categoryName,
        boolean isFeatured
) {
}
