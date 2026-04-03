package com.mockhub.tmexplorer.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record EventResponse(
        String id,
        String name,
        String url,
        Dates dates,
        Sales sales,
        Ticketing ticketing,
        DoorsTimes doorsTimes,
        List<Classification> classifications,
        List<Image> images,
        List<PriceRange> priceRanges,
        List<Product> products,
        Promoter promoter,
        String info,
        String pleaseNote,
        @JsonProperty("_embedded") Embedded embedded
) {

    // --- Dates ---

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Dates(
            Start start,
            String timezone,
            Status status,
            Boolean spanMultipleDays
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

    // --- Sales ---

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Sales(
            PublicSale publicSale,
            List<Presale> presales
    ) {
        // "public" is a reserved word in Java — use @JsonProperty
        @JsonProperty("public")
        public PublicSale publicSale() {
            return publicSale;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PublicSale(
            String startDateTime,
            String endDateTime,
            Boolean startTBD,
            Boolean startTBA
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Presale(
            String startDateTime,
            String endDateTime,
            String name
    ) {
    }

    // --- Ticketing ---

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Ticketing(
            SafeTix safeTix,
            AllInclusivePricing allInclusivePricing
    ) {
        public boolean isAllInclusivePricing() {
            return allInclusivePricing != null && Boolean.TRUE.equals(allInclusivePricing.enabled());
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SafeTix(Boolean enabled) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record AllInclusivePricing(Boolean enabled) {
    }

    // --- Classification ---

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Classification(
            Boolean primary,
            Segment segment,
            Genre genre,
            SubGenre subGenre
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Segment(String id, String name) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Genre(String id, String name) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SubGenre(String id, String name) {
    }

    // --- Images ---

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Image(
            String url,
            String ratio,
            Integer width,
            Integer height,
            Boolean fallback
    ) {
    }

    // --- Pricing ---

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PriceRange(
            String type,
            String currency,
            Double min,
            Double max
    ) {
    }

    // --- Products (linked items like parking, VIP packages) ---

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Product(
            String name,
            String id,
            String url,
            String type
    ) {
    }

    // --- Promoter ---

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Promoter(
            String id,
            String name,
            String description
    ) {
    }

    // --- Embedded ---

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Embedded(
            List<VenueResponse> venues,
            List<AttractionResponse> attractions
    ) {
    }

    // --- Convenience methods ---

    /**
     * Whether this event uses all-inclusive pricing (fees baked in, no priceRanges).
     */
    public boolean isAllInclusivePricing() {
        return ticketing != null && ticketing.isAllInclusivePricing();
    }

    /**
     * Whether this event has any pricing info available from the Discovery API.
     */
    public boolean hasPricing() {
        return priceRanges != null && !priceRanges.isEmpty();
    }

    /**
     * Human-readable pricing status.
     */
    public String pricingStatus() {
        if (hasPricing()) {
            PriceRange pr = priceRanges.getFirst();
            return "$%.2f - $%.2f %s".formatted(
                    pr.min() != null ? pr.min() : 0.0,
                    pr.max() != null ? pr.max() : 0.0,
                    pr.currency());
        } else if (isAllInclusivePricing()) {
            return "ALL-INCLUSIVE (price not in Discovery API)";
        } else {
            return "NONE";
        }
    }
}
