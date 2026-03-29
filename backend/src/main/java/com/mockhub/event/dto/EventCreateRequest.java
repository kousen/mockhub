package com.mockhub.event.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

@Schema(description = "Request to create or update an event")
public record EventCreateRequest(
        @NotBlank(message = "Event name is required")
        @Schema(description = "Event name", example = "Taylor Swift - Eras Tour")
        String name,

        @NotNull(message = "Venue ID is required")
        @Schema(description = "Venue ID", example = "1")
        Long venueId,

        @NotNull(message = "Category ID is required")
        @Schema(description = "Category ID", example = "1")
        Long categoryId,

        @NotNull(message = "Event date is required")
        @Schema(description = "Event date/time in ISO-8601", example = "2026-09-15T20:00:00Z")
        Instant eventDate,

        @NotNull(message = "Base price is required")
        @Positive(message = "Base price must be positive")
        @Schema(description = "Base ticket price", example = "150.00")
        BigDecimal basePrice,

        @Schema(description = "Event description")
        String description,
        @Schema(description = "Performing artist/team name", example = "Taylor Swift")
        String artistName,
        @Schema(description = "Doors open time")
        Instant doorsOpenAt,
        @Schema(description = "Tag IDs to associate")
        List<Long> tagIds,
        @Schema(description = "Whether this event is featured on the homepage")
        Boolean isFeatured,
        @Pattern(regexp = "^[a-zA-Z0-9]{22}$", message = "Invalid Spotify artist ID format")
        @Schema(description = "Spotify artist ID for 'Listen on Spotify' link", example = "06HL4z0CvFAxyc27GXpf02")
        String spotifyArtistId
) {
}
