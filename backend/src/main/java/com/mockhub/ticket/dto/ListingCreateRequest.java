package com.mockhub.ticket.dto;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ListingCreateRequest(
        @NotNull(message = "Ticket ID is required")
        Long ticketId,

        @NotNull(message = "Listed price is required")
        @Positive(message = "Listed price must be positive")
        BigDecimal listedPrice,

        Instant expiresAt
) {
}
