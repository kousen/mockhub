package com.mockhub.venue.dto;

import java.math.BigDecimal;

public record SectionDto(
        Long id,
        String name,
        String sectionType,
        int capacity,
        int sortOrder,
        String svgPathId,
        BigDecimal svgX,
        BigDecimal svgY,
        BigDecimal svgWidth,
        BigDecimal svgHeight,
        String colorHex,
        int totalRows,
        int totalSeats
) {
}
