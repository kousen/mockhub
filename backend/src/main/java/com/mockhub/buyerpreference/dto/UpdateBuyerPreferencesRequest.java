package com.mockhub.buyerpreference.dto;

import java.math.BigDecimal;
import java.util.List;

import com.mockhub.buyerpreference.entity.RiskTolerance;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;

public record UpdateBuyerPreferencesRequest(
        @Size(max = 20)
        List<String> preferredArtists,
        @Size(max = 20)
        List<String> preferredCategories,
        @Size(max = 20)
        List<String> preferredCities,
        @Size(max = 20)
        List<String> preferredVenues,
        @Size(max = 20)
        List<String> preferredSections,
        @Size(max = 20)
        List<String> dislikedVenues,
        @Size(max = 20)
        List<String> dislikedCategories,
        @DecimalMin("0.00") BigDecimal maxTotalPrice,
        @DecimalMin("0.00") @DecimalMax("100.00") BigDecimal maxServiceFeePercent,
        boolean allInPriceOnly,
        @Size(max = 1000)
        String accessibilityNeeds,
        RiskTolerance riskTolerance,
        boolean willingToWaitForPriceDrops
) {
}
