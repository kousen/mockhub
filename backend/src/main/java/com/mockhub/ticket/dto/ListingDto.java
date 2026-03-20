package com.mockhub.ticket.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record ListingDto(
        Long id,
        Long ticketId,
        String eventSlug,
        String sectionName,
        String rowLabel,
        String seatNumber,
        String ticketType,
        BigDecimal listedPrice,
        BigDecimal computedPrice,
        BigDecimal priceMultiplier,
        String status,
        Instant listedAt,
        String sellerDisplayName
) {
}
