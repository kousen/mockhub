package com.mockhub.venue.dto;

import java.math.BigDecimal;
import java.util.List;

public record VenueDto(
        Long id,
        String name,
        String slug,
        String addressLine1,
        String addressLine2,
        String city,
        String state,
        String zipCode,
        String country,
        BigDecimal latitude,
        BigDecimal longitude,
        int capacity,
        String venueType,
        String imageUrl,
        String svgMapUrl,
        List<SectionDto> sections
) {
}
