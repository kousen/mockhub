package com.mockhub.ai.dto;

import java.util.List;

import com.mockhub.buyerpreference.dto.BuyerPreferenceContextDto;

public record RecommendationsResponse(
        List<RecommendationDto> recommendations,
        boolean spotifyConnected,
        boolean scopeUpgradeNeeded,
        BuyerPreferenceContextDto preferenceContext
) {

    public RecommendationsResponse(List<RecommendationDto> recommendations,
                                   boolean spotifyConnected,
                                   boolean scopeUpgradeNeeded) {
        this(recommendations, spotifyConnected, scopeUpgradeNeeded, BuyerPreferenceContextDto.none());
    }
}
