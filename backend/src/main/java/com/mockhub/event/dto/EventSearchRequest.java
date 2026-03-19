package com.mockhub.event.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

public record EventSearchRequest(
        String q,
        String category,
        String tags,
        String city,
        Instant dateFrom,
        Instant dateTo,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        String status,
        String sort,
        Integer page,
        Integer size
) {

    public EventSearchRequest {
        status = Objects.requireNonNullElse(status, "ACTIVE");
        sort = Objects.requireNonNullElse(sort, "eventDate");
        page = (page == null || page < 0) ? 0 : page;
        size = (size == null || size <= 0) ? 20 : Math.min(size, 100);
    }
}
