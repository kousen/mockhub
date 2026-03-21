package com.mockhub.ticket.dto;

import java.time.Instant;

public record TicketVerificationResult(
        boolean valid,
        String orderNumber,
        Long ticketId,
        String eventSlug,
        String sectionName,
        String rowLabel,
        String seatNumber,
        boolean alreadyScanned,
        Instant scannedAt,
        String message
) {
}
