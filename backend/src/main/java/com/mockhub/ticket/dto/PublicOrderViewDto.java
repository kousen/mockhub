package com.mockhub.ticket.dto;

import java.util.List;

public record PublicOrderViewDto(
        String orderNumber,
        String status,
        String eventName,
        String eventDate,
        String venueName,
        String venueLocation,
        List<PublicTicketViewDto> tickets
) {
}
