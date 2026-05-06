package com.mockhub.ticket.dto;

import java.util.List;

public record TicketComparisonResponseDto(
        int totalMatches,
        String judgmentSource,
        String comparisonBasis,
        TicketComparisonOptionDto cheapestOption,
        TicketComparisonOptionDto bestValueOption,
        TicketComparisonOptionDto bestSectionOption,
        TicketComparisonOptionDto lowestRiskOption,
        List<TicketComparisonOptionDto> rankedOptions,
        List<PriceWarningDto> priceWarnings
) {
}
