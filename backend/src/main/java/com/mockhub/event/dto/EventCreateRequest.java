package com.mockhub.event.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record EventCreateRequest(
        @NotBlank(message = "Event name is required")
        String name,

        @NotNull(message = "Venue ID is required")
        Long venueId,

        @NotNull(message = "Category ID is required")
        Long categoryId,

        @NotNull(message = "Event date is required")
        Instant eventDate,

        @NotNull(message = "Base price is required")
        @Positive(message = "Base price must be positive")
        BigDecimal basePrice,

        String description,
        String artistName,
        Instant doorsOpenAt,
        List<Long> tagIds,
        Boolean isFeatured
) {
}
