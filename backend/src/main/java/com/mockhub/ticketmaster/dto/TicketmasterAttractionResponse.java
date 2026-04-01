package com.mockhub.ticketmaster.dto;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TicketmasterAttractionResponse(
        String id,
        String name,
        Map<String, List<ExternalLink>> externalLinks
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ExternalLink(
            String url,
            String id
    ) {
    }
}
