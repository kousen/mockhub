package com.mockhub.admin.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import com.mockhub.event.dto.CategoryDto;
import com.mockhub.event.dto.TagDto;
import com.mockhub.venue.dto.VenueSummaryDto;

public record AdminEventDto(
        Long id,
        String name,
        String slug,
        String description,
        String artistName,
        Instant eventDate,
        Instant doorsOpenAt,
        String status,
        BigDecimal basePrice,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        int totalTickets,
        int availableTickets,
        boolean isFeatured,
        VenueSummaryDto venue,
        CategoryDto category,
        List<TagDto> tags,
        String primaryImageUrl,
        int soldTickets,
        BigDecimal revenue
) {
}
