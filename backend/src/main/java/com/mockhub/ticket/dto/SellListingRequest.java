package com.mockhub.ticket.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record SellListingRequest(
        @NotBlank(message = "Event slug is required")
        String eventSlug,

        @NotBlank(message = "Section name is required")
        String sectionName,

        @NotBlank(message = "Row label is required")
        String rowLabel,

        @NotBlank(message = "Seat number is required")
        String seatNumber,

        @NotNull(message = "Price is required")
        @Positive(message = "Price must be positive")
        BigDecimal price
) {
}
