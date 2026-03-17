package com.mockhub.search.dto;

import com.mockhub.event.dto.EventSummaryDto;

public record SearchResultDto(
        EventSummaryDto event,
        double relevanceScore
) {
}
