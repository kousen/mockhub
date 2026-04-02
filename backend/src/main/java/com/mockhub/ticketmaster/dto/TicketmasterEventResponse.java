package com.mockhub.ticketmaster.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TicketmasterEventResponse(
        String id,
        String name,
        String url,
        Dates dates,
        List<Classification> classifications,
        List<Image> images,
        List<PriceRange> priceRanges,
        Ticketing ticketing,
        DoorsTimes doorsTimes,
        @JsonProperty("_embedded") Embedded embedded
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Dates(
            Start start,
            String timezone,
            Status status
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Start(
            String localDate,
            String localTime,
            String dateTime,
            Boolean dateTBD,
            Boolean timeTBA
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Status(
            String code
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record DoorsTimes(
            String localDate,
            String localTime,
            String dateTime
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Ticketing(
            AllInclusivePricing allInclusivePricing
    ) {
        public boolean isAllInclusivePricing() {
            return allInclusivePricing != null
                    && Boolean.TRUE.equals(allInclusivePricing.enabled());
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record AllInclusivePricing(Boolean enabled) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Classification(
            Boolean primary,
            Segment segment,
            Genre genre,
            SubGenre subGenre
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Segment(
            String id,
            String name
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Genre(
            String id,
            String name
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SubGenre(
            String id,
            String name
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Image(
            String url,
            String ratio,
            Integer width,
            Integer height,
            Boolean fallback
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PriceRange(
            String type,
            String currency,
            Double min,
            Double max
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Embedded(
            List<TicketmasterVenueResponse> venues,
            List<TicketmasterAttractionResponse> attractions
    ) {
    }

    /**
     * Whether this event uses all-inclusive pricing (fees baked in).
     * Events with AIP never have priceRanges in the Discovery API.
     */
    public boolean isAllInclusivePricing() {
        return ticketing != null && ticketing.isAllInclusivePricing();
    }
}
