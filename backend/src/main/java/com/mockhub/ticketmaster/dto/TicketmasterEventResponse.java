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

    /**
     * Builder for TicketmasterEventResponse. Avoids fragile 10-argument positional constructors
     * in test code. All fields default to null so callers only set what they need.
     */
    public static final class Builder {
        private String id;
        private String name;
        private String url;
        private Dates dates;
        private List<Classification> classifications;
        private List<Image> images;
        private List<PriceRange> priceRanges;
        private Ticketing ticketing;
        private DoorsTimes doorsTimes;
        private Embedded embedded;

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder url(String url) {
            this.url = url;
            return this;
        }

        public Builder dates(Dates dates) {
            this.dates = dates;
            return this;
        }

        public Builder classifications(List<Classification> classifications) {
            this.classifications = classifications;
            return this;
        }

        public Builder images(List<Image> images) {
            this.images = images;
            return this;
        }

        public Builder priceRanges(List<PriceRange> priceRanges) {
            this.priceRanges = priceRanges;
            return this;
        }

        public Builder ticketing(Ticketing ticketing) {
            this.ticketing = ticketing;
            return this;
        }

        public Builder doorsTimes(DoorsTimes doorsTimes) {
            this.doorsTimes = doorsTimes;
            return this;
        }

        public Builder embedded(Embedded embedded) {
            this.embedded = embedded;
            return this;
        }

        public TicketmasterEventResponse build() {
            return new TicketmasterEventResponse(
                    id, name, url, dates, classifications, images,
                    priceRanges, ticketing, doorsTimes, embedded);
        }
    }

    public static Builder builder() {
        return new Builder();
    }
}
