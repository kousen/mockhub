package com.mockhub.ticket.dto;

import java.math.BigDecimal;
import java.util.List;

public record EarningsSummaryDto(
        BigDecimal totalEarnings,
        long totalListings,
        long activeListings,
        long soldListings,
        List<SaleDto> recentSales
) {
}
