package com.mockhub.venue.dto;

import java.math.BigDecimal;

public record SectionAvailabilityDto(
        Long sectionId,
        String sectionName,
        String sectionType,
        int totalTickets,
        int availableTickets,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        String colorHex
) {
}
