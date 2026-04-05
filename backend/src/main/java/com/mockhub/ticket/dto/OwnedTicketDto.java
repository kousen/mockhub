package com.mockhub.ticket.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record OwnedTicketDto(
        Long ticketId,
        String eventSlug,
        String eventName,
        Instant eventDate,
        String venueName,
        String sectionName,
        String rowLabel,
        String seatNumber,
        String ticketType,
        BigDecimal faceValue
) {
}
