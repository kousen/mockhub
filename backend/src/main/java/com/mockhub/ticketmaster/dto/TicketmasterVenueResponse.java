package com.mockhub.ticketmaster.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TicketmasterVenueResponse(
        String id,
        String name,
        Address address,
        City city,
        State state,
        String postalCode,
        Country country,
        Location location
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Address(
            String line1
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record City(
            String name
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record State(
            String name,
            String stateCode
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Country(
            String name,
            String countryCode
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Location(
            String latitude,
            String longitude
    ) {
    }
}
