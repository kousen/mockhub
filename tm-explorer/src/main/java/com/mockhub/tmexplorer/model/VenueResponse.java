package com.mockhub.tmexplorer.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record VenueResponse(
        String id,
        String name,
        String postalCode,
        String timezone,
        Address address,
        City city,
        State state,
        Country country,
        Location location
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Address(String line1) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record City(String name) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record State(String name, String stateCode) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Country(String name, String countryCode) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Location(String latitude, String longitude) {
    }
}
