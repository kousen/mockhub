package com.mockhub.pricing.dto;

import java.util.List;

/**
 * Bounded price-history response for agent-facing tools. Snapshots are
 * newest-first; totalSnapshots tells the caller how much history exists
 * beyond the returned window.
 */
public record PriceHistoryPageDto(
        List<PriceHistoryDto> snapshots,
        int returned,
        long totalSnapshots,
        int limit
) {
}
