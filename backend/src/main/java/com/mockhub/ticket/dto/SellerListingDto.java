package com.mockhub.ticket.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record SellerListingDto(
        Long id,
        Long ticketId,
        String eventSlug,
        String eventName,
        Instant eventDate,
        String venueName,
        String sectionName,
        String rowLabel,
        String seatNumber,
        String ticketType,
        BigDecimal listedPrice,
        BigDecimal computedPrice,
        BigDecimal faceValue,
        String status,
        Instant listedAt,
        Instant createdAt
) {
}
