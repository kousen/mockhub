package com.mockhub.tmexplorer.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

/**
 * Tests Jackson 3 deserialization of Ticketmaster Discovery API responses.
 * Uses saved JSON fixtures to verify parsing without hitting the live API.
 */
class DeserializationTest {

    private static JsonMapper jsonMapper;

    @BeforeAll
    static void setUp() {
        jsonMapper = JsonMapper.builder()
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .build();
    }

    private String loadFixture(String filename) throws Exception {
        try (InputStream is = getClass().getResourceAsStream("/" + filename)) {
            assertThat(is).as("Fixture file %s must exist", filename).isNotNull();
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @Nested
    @DisplayName("Event with Spotify links")
    class EventWithSpotify {

        @Test
        @DisplayName("should deserialize SearchResponse with embedded events")
        void searchResponse_deserializes() throws Exception {
            String json = loadFixture("sample-event-with-spotify.json");
            SearchResponse response = jsonMapper.readValue(json, SearchResponse.class);

            assertThat(response).isNotNull();
            assertThat(response.embedded()).isNotNull();
            assertThat(response.embedded().events()).hasSize(1);
            assertThat(response.page().totalElements()).isEqualTo(42);
        }

        @Test
        @DisplayName("should deserialize event fields correctly")
        void eventFields_deserialize() throws Exception {
            String json = loadFixture("sample-event-with-spotify.json");
            SearchResponse response = jsonMapper.readValue(json, SearchResponse.class);
            EventResponse event = response.embedded().events().getFirst();

            assertThat(event.name()).isEqualTo("Beyoncé - Cowboy Carter Tour");
            assertThat(event.id()).isEqualTo("vvG1fZ9pBkdiRe");
            assertThat(event.dates().start().localDate()).isEqualTo("2026-07-15");
            assertThat(event.dates().status().code()).isEqualTo("onsale");
        }

        @Test
        @DisplayName("should deserialize price ranges")
        void priceRanges_deserialize() throws Exception {
            String json = loadFixture("sample-event-with-spotify.json");
            EventResponse event = jsonMapper.readValue(json, SearchResponse.class)
                    .embedded().events().getFirst();

            assertThat(event.priceRanges()).hasSize(1);
            assertThat(event.priceRanges().getFirst().min()).isEqualTo(89.50);
            assertThat(event.priceRanges().getFirst().max()).isEqualTo(450.00);
            assertThat(event.priceRanges().getFirst().currency()).isEqualTo("USD");
        }

        @Test
        @DisplayName("should deserialize venue from _embedded")
        void venue_deserializes() throws Exception {
            String json = loadFixture("sample-event-with-spotify.json");
            EventResponse event = jsonMapper.readValue(json, SearchResponse.class)
                    .embedded().events().getFirst();

            assertThat(event.embedded().venues()).hasSize(1);
            VenueResponse venue = event.embedded().venues().getFirst();
            assertThat(venue.name()).isEqualTo("Madison Square Garden");
            assertThat(venue.city().name()).isEqualTo("New York");
            assertThat(venue.state().stateCode()).isEqualTo("NY");
        }

        @Test
        @DisplayName("should deserialize attraction with externalLinks as ExternalLink records")
        void attraction_externalLinks_deserialize() throws Exception {
            String json = loadFixture("sample-event-with-spotify.json");
            EventResponse event = jsonMapper.readValue(json, SearchResponse.class)
                    .embedded().events().getFirst();

            assertThat(event.embedded().attractions()).hasSize(1);
            AttractionResponse attraction = event.embedded().attractions().getFirst();

            assertThat(attraction.name()).isEqualTo("Beyoncé");
            assertThat(attraction.externalLinks()).isNotNull();
            assertThat(attraction.externalLinks()).containsKey("spotify");

            List<AttractionResponse.ExternalLink> spotifyLinks =
                    attraction.externalLinks().get("spotify");
            assertThat(spotifyLinks).hasSize(1);
            assertThat(spotifyLinks.getFirst().url())
                    .isEqualTo("https://open.spotify.com/artist/6vWDO969PvNqNYHIOW5v0m");
        }

        @Test
        @DisplayName("should deserialize all external link platforms")
        void allExternalLinks_deserialize() throws Exception {
            String json = loadFixture("sample-event-with-spotify.json");
            AttractionResponse attraction = jsonMapper.readValue(json, SearchResponse.class)
                    .embedded().events().getFirst()
                    .embedded().attractions().getFirst();

            assertThat(attraction.externalLinks().keySet())
                    .containsExactlyInAnyOrder(
                            "youtube", "twitter", "itunes", "spotify",
                            "facebook", "instagram", "homepage");
        }
    }

    @Nested
    @DisplayName("Event without Spotify links")
    class EventWithoutSpotify {

        @Test
        @DisplayName("should deserialize event with no spotify in externalLinks")
        void noSpotify_deserializes() throws Exception {
            String json = loadFixture("sample-event-no-spotify.json");
            AttractionResponse attraction = jsonMapper.readValue(json, SearchResponse.class)
                    .embedded().events().getFirst()
                    .embedded().attractions().getFirst();

            assertThat(attraction.name()).isEqualTo("New York Knicks");
            assertThat(attraction.externalLinks()).doesNotContainKey("spotify");
            assertThat(attraction.externalLinks()).containsKey("twitter");
        }
    }

    @Nested
    @DisplayName("Event without price ranges")
    class EventWithoutPrices {

        @Test
        @DisplayName("should handle null priceRanges gracefully")
        void nullPriceRanges_deserializes() throws Exception {
            String json = loadFixture("sample-event-no-prices.json");
            EventResponse event = jsonMapper.readValue(json, SearchResponse.class)
                    .embedded().events().getFirst();

            assertThat(event.priceRanges()).isNull();
        }

        @Test
        @DisplayName("should still extract spotify from event without prices")
        void spotifyWithoutPrices_works() throws Exception {
            String json = loadFixture("sample-event-no-prices.json");
            AttractionResponse attraction = jsonMapper.readValue(json, SearchResponse.class)
                    .embedded().events().getFirst()
                    .embedded().attractions().getFirst();

            assertThat(attraction.name()).isEqualTo("Taylor Swift");
            assertThat(attraction.externalLinks().get("spotify").getFirst().url())
                    .contains("06HL4z0CvFAxyc27GXpf02");
        }
    }

    @Nested
    @DisplayName("All-inclusive pricing event (no priceRanges)")
    class AllInclusivePricing {

        @Test
        @DisplayName("should deserialize ticketing with allInclusivePricing")
        void allInclusivePricing_deserializes() throws Exception {
            String json = loadFixture("sample-event-all-inclusive.json");
            EventResponse event = jsonMapper.readValue(json, SearchResponse.class)
                    .embedded().events().getFirst();

            assertThat(event.ticketing()).isNotNull();
            assertThat(event.ticketing().allInclusivePricing().enabled()).isTrue();
            assertThat(event.isAllInclusivePricing()).isTrue();
            assertThat(event.hasPricing()).isFalse();
        }

        @Test
        @DisplayName("should report correct pricing status for all-inclusive event")
        void pricingStatus_allInclusive() throws Exception {
            String json = loadFixture("sample-event-all-inclusive.json");
            EventResponse event = jsonMapper.readValue(json, SearchResponse.class)
                    .embedded().events().getFirst();

            assertThat(event.pricingStatus()).contains("ALL-INCLUSIVE");
        }

        @Test
        @DisplayName("should deserialize sales with public and presales")
        void sales_deserialize() throws Exception {
            String json = loadFixture("sample-event-all-inclusive.json");
            EventResponse event = jsonMapper.readValue(json, SearchResponse.class)
                    .embedded().events().getFirst();

            assertThat(event.sales()).isNotNull();
            assertThat(event.sales().publicSale()).isNotNull();
            assertThat(event.sales().publicSale().startDateTime())
                    .isEqualTo("2026-02-20T18:00:00Z");
            assertThat(event.sales().presales()).hasSize(2);
            assertThat(event.sales().presales().getFirst().name())
                    .isEqualTo("Artist Presale");
        }

        @Test
        @DisplayName("should deserialize doorsTimes")
        void doorsTimes_deserialize() throws Exception {
            String json = loadFixture("sample-event-all-inclusive.json");
            EventResponse event = jsonMapper.readValue(json, SearchResponse.class)
                    .embedded().events().getFirst();

            assertThat(event.doorsTimes()).isNotNull();
            assertThat(event.doorsTimes().localTime()).isEqualTo("18:30:00");
        }

        @Test
        @DisplayName("should deserialize products (parking, VIP)")
        void products_deserialize() throws Exception {
            String json = loadFixture("sample-event-all-inclusive.json");
            EventResponse event = jsonMapper.readValue(json, SearchResponse.class)
                    .embedded().events().getFirst();

            assertThat(event.products()).hasSize(1);
            assertThat(event.products().getFirst().name()).contains("Parking");
            assertThat(event.products().getFirst().type()).isEqualTo("Parking");
        }

        @Test
        @DisplayName("should deserialize promoter")
        void promoter_deserializes() throws Exception {
            String json = loadFixture("sample-event-all-inclusive.json");
            EventResponse event = jsonMapper.readValue(json, SearchResponse.class)
                    .embedded().events().getFirst();

            assertThat(event.promoter()).isNotNull();
            assertThat(event.promoter().name()).isEqualTo("LIVE NATION MUSIC");
        }

        @Test
        @DisplayName("should still extract Spotify from all-inclusive event")
        void spotify_worksWithAllInclusive() throws Exception {
            String json = loadFixture("sample-event-all-inclusive.json");
            AttractionResponse attraction = jsonMapper.readValue(json, SearchResponse.class)
                    .embedded().events().getFirst()
                    .embedded().attractions().getFirst();

            assertThat(attraction.name()).isEqualTo("Eagles");
            assertThat(attraction.externalLinks().get("spotify").getFirst().url())
                    .contains("0ECwFtbIWEVNwjlrfc6xoL");
        }
    }

    @Nested
    @DisplayName("Pricing convenience methods")
    class PricingMethods {

        @Test
        @DisplayName("hasPricing returns true when priceRanges exist")
        void hasPricing_withPrices() throws Exception {
            String json = loadFixture("sample-event-with-spotify.json");
            EventResponse event = jsonMapper.readValue(json, SearchResponse.class)
                    .embedded().events().getFirst();

            assertThat(event.hasPricing()).isTrue();
            assertThat(event.isAllInclusivePricing()).isFalse();
            assertThat(event.pricingStatus()).contains("$89.50");
        }

        @Test
        @DisplayName("pricingStatus returns NONE when no pricing info at all")
        void pricingStatus_none() throws Exception {
            String json = loadFixture("sample-event-no-prices.json");
            EventResponse event = jsonMapper.readValue(json, SearchResponse.class)
                    .embedded().events().getFirst();

            assertThat(event.hasPricing()).isFalse();
            assertThat(event.isAllInclusivePricing()).isFalse();
            assertThat(event.pricingStatus()).isEqualTo("NONE");
        }
    }
}
