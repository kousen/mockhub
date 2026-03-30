package com.mockhub.ticket.dto;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Encapsulates all search filters for the findTickets compound query.
 * Null fields are ignored (no predicate added), eliminating the
 * Hibernate nullable-parameter type inference issue.
 */
public record ListingSearchCriteria(
        String query,
        String categorySlug,
        String city,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        String section,
        Instant dateFrom,
        Instant dateTo,
        int limit
) {

    public ListingSearchCriteria {
        if (dateFrom == null) {
            dateFrom = Instant.now();
        }
        if (limit <= 0) {
            limit = 10;
        }
    }
}
