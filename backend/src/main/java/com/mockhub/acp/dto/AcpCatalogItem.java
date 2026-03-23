package com.mockhub.acp.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record AcpCatalogItem(
        String productId,
        String name,
        String description,
        String category,
        String venue,
        String city,
        Instant eventDate,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        int availableTickets,
        String url
) {
}
