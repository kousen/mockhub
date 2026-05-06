package com.mockhub.ticket.dto;

import java.util.List;

public record TicketComparisonOptionDto(
        int rank,
        TicketSearchResultDto listing,
        int score,
        List<String> reasonCodes,
        List<String> warnings,
        String rationale
) {
}
