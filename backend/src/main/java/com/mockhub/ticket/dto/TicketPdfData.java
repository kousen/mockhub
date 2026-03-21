package com.mockhub.ticket.dto;

public record TicketPdfData(
        String eventName,
        String eventDateFormatted,
        String doorsOpenFormatted,
        String venueName,
        String venueLocation,
        String sectionName,
        String rowLabel,
        String seatNumber,
        String ticketType,
        String pricePaid,
        String orderNumber,
        String buyerName,
        byte[] qrCodeImage
) {
}
