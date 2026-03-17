package com.mockhub.venue.dto;

public record VenueSummaryDto(
        Long id,
        String name,
        String slug,
        String city,
        String state,
        String venueType,
        int capacity,
        String imageUrl
) {
}
