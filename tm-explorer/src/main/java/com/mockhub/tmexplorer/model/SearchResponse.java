package com.mockhub.tmexplorer.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SearchResponse(
        @JsonProperty("_embedded") Embedded embedded,
        PageInfo page
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Embedded(
            List<EventResponse> events
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PageInfo(
            int size,
            int totalElements,
            int totalPages,
            int number
    ) {
    }
}
