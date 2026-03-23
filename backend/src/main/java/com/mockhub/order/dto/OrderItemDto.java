package com.mockhub.order.dto;

import java.math.BigDecimal;

public record OrderItemDto(
        Long id,
        Long listingId,
        Long ticketId,
        String eventName,
        String eventSlug,
        String sectionName,
        String rowLabel,
        String seatNumber,
        String ticketType,
        BigDecimal pricePaid
) {
}
