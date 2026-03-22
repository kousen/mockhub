package com.mockhub.ticket.dto;

public record PublicTicketViewDto(
        Long ticketId,
        String sectionName,
        String rowLabel,
        String seatNumber,
        String ticketType,
        String qrCodeUrl
) {
}
