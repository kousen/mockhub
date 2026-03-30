package com.mockhub.ai.dto;

import java.util.List;

public record RecommendationsResponse(
        List<RecommendationDto> recommendations,
        boolean spotifyConnected,
        boolean scopeUpgradeNeeded
) {
}
