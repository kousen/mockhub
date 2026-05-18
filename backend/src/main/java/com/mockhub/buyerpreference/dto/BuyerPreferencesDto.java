package com.mockhub.buyerpreference.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import com.mockhub.buyerpreference.entity.RiskTolerance;

public record BuyerPreferencesDto(
        Long id,
        Long userId,
        List<String> preferredArtists,
        List<String> preferredCategories,
        List<String> preferredCities,
        List<String> preferredVenues,
        List<String> preferredSections,
        List<String> dislikedVenues,
        List<String> dislikedCategories,
        BigDecimal maxTotalPrice,
        BigDecimal maxServiceFeePercent,
        boolean allInPriceOnly,
        String accessibilityNeeds,
        RiskTolerance riskTolerance,
        boolean willingToWaitForPriceDrops,
        Instant createdAt,
        Instant updatedAt
) {
}
