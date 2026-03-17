package com.mockhub.cart.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record CartItemDto(
        Long id,
        Long listingId,
        String eventName,
        String eventSlug,
        String sectionName,
        String rowLabel,
        String seatNumber,
        String ticketType,
        BigDecimal priceAtAdd,
        BigDecimal currentPrice,
        Instant addedAt
) {
}
