package com.mockhub.ticket.dto;

public record PriceWarningDto(
        Long listingId,
        String code,
        String message,
        String source
) {
}
