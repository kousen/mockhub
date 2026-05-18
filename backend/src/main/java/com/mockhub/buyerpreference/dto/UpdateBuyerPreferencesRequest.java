package com.mockhub.buyerpreference.dto;

import java.math.BigDecimal;
import java.util.List;

import com.mockhub.buyerpreference.entity.RiskTolerance;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;

public record UpdateBuyerPreferencesRequest(
        List<String> preferredArtists,
        List<String> preferredCategories,
        List<String> preferredCities,
        List<String> preferredVenues,
        List<String> preferredSections,
        List<String> dislikedVenues,
        List<String> dislikedCategories,
        @DecimalMin("0.00") BigDecimal maxTotalPrice,
        @DecimalMin("0.00") @DecimalMax("100.00") BigDecimal maxServiceFeePercent,
        boolean allInPriceOnly,
        String accessibilityNeeds,
        RiskTolerance riskTolerance,
        boolean willingToWaitForPriceDrops
) {
}
