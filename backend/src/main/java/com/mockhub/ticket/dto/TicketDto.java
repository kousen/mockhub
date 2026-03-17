package com.mockhub.ticket.dto;

import java.math.BigDecimal;

public record TicketDto(
        Long id,
        Long eventId,
        String sectionName,
        String rowLabel,
        String seatNumber,
        String ticketType,
        BigDecimal faceValue,
        String status
) {
}
