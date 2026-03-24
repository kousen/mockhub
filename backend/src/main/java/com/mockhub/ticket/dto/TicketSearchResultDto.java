package com.mockhub.ticket.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record TicketSearchResultDto(
        Long listingId,
        Long ticketId,
        String eventName,
        String eventSlug,
        String artistName,
        String categoryName,
        String venueName,
        String city,
        Instant eventDate,
        String sectionName,
        String rowLabel,
        String seatNumber,
        String ticketType,
        BigDecimal price,
        String sellerDisplayName
) {
}
