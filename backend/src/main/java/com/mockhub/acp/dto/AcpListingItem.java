package com.mockhub.acp.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record AcpListingItem(
        Long listingId,
        String productId,
        String eventName,
        String eventSlug,
        String artistName,
        String category,
        String venue,
        String city,
        Instant eventDate,
        String sectionName,
        String rowLabel,
        String seatNumber,
        BigDecimal price,
        String url
) {
}
