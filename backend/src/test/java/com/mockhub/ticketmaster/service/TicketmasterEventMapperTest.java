package com.mockhub.ticketmaster.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.mockhub.event.entity.Category;
import com.mockhub.ticketmaster.dto.TicketmasterAttractionResponse;
import com.mockhub.ticketmaster.dto.TicketmasterAttractionResponse.ExternalLink;
import com.mockhub.ticketmaster.dto.TicketmasterEventResponse;
import com.mockhub.ticketmaster.dto.TicketmasterEventResponse.Classification;
import com.mockhub.ticketmaster.dto.TicketmasterEventResponse.Dates;
import com.mockhub.ticketmaster.dto.TicketmasterEventResponse.DoorsTimes;
import com.mockhub.ticketmaster.dto.TicketmasterEventResponse.Embedded;
import com.mockhub.ticketmaster.dto.TicketmasterEventResponse.Genre;
import com.mockhub.ticketmaster.dto.TicketmasterEventResponse.Image;
import com.mockhub.ticketmaster.dto.TicketmasterEventResponse.PriceRange;
import com.mockhub.ticketmaster.dto.TicketmasterEventResponse.Segment;
import com.mockhub.ticketmaster.dto.TicketmasterEventResponse.Start;
import com.mockhub.ticketmaster.dto.TicketmasterEventResponse.Status;
import com.mockhub.ticketmaster.dto.TicketmasterEventResponse.SubGenre;
import com.mockhub.ticketmaster.dto.TicketmasterVenueResponse;
import com.mockhub.venue.entity.Venue;

import com.mockhub.event.entity.Event;

import static org.assertj.core.api.Assertions.assertThat;

class TicketmasterEventMapperTest {

    private TicketmasterEventMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new TicketmasterEventMapper();
    }

    // --- Category resolution ---

    @Test
    void resolveCategory_givenMusicSegment_returnsConcerts() {
        List<Classification> classifications = List.of(
                new Classification(true,
                        new Segment("KZFzniwnSyZfZ7v7nJ", "Music"),
                        new Genre("1", "Rock"),
                        new SubGenre("1", "Pop")));

        assertThat(mapper.resolveCategorySlug(classifications)).isEqualTo("concerts");
    }

    @Test
    void resolveCategory_givenSportsSegment_returnsSports() {
        List<Classification> classifications = List.of(
                new Classification(true,
                        new Segment("KZFzniwnSyZfZ7v7nE", "Sports"),
                        new Genre("1", "Basketball"),
                        new SubGenre("1", "NBA")));

        assertThat(mapper.resolveCategorySlug(classifications)).isEqualTo("sports");
    }

    @Test
    void resolveCategory_givenArtsAndTheatreSegment_returnsTheater() {
        List<Classification> classifications = List.of(
                new Classification(true,
                        new Segment("KZFzniwnSyZfZ7v7na", "Arts & Theatre"),
                        new Genre("1", "Theatre"),
                        new SubGenre("1", "Musical")));

        assertThat(mapper.resolveCategorySlug(classifications)).isEqualTo("theater");
    }

    @Test
    void resolveCategory_givenComedyGenreInArtsSegment_returnsComedy() {
        List<Classification> classifications = List.of(
                new Classification(true,
                        new Segment("KZFzniwnSyZfZ7v7na", "Arts & Theatre"),
                        new Genre("1", "Comedy"),
                        new SubGenre("1", "Stand-Up")));

        assertThat(mapper.resolveCategorySlug(classifications)).isEqualTo("comedy");
    }

    @Test
    void resolveCategory_givenNullPrimaryFlag_usesFirstClassification() {
        List<Classification> classifications = List.of(
                new Classification(null,
                        new Segment("KZFzniwnSyZfZ7v7nJ", "Music"),
                        new Genre("1", "Rock"),
                        new SubGenre("1", "Pop")));

        assertThat(mapper.resolveCategorySlug(classifications)).isEqualTo("concerts");
    }

    @Test
    void resolveCategory_givenNullClassifications_returnsOther() {
        assertThat(mapper.resolveCategorySlug(null)).isEqualTo("other");
    }

    @Test
    void resolveCategory_givenEmptyClassifications_returnsOther() {
        assertThat(mapper.resolveCategorySlug(List.of())).isEqualTo("other");
    }

    // --- Spotify extraction ---

    @Nested
    class SpotifyExtraction {

        @Test
        void extractSpotifyArtistId_givenStandardUrl_returnsArtistId() {
            TicketmasterAttractionResponse attraction = new TicketmasterAttractionResponse(
                    "K8vZ9171ob7", "Eagles",
                    Map.of("spotify", List.of(
                            new ExternalLink("https://open.spotify.com/artist/0ECwFtbIWEVNwjlrfc6xoL", null))));

            assertThat(mapper.extractSpotifyArtistId(attraction)).isEqualTo("0ECwFtbIWEVNwjlrfc6xoL");
        }

        @Test
        void extractSpotifyArtistId_givenMangledUri_recoversArtistId() {
            TicketmasterAttractionResponse attraction = new TicketmasterAttractionResponse(
                    "K8vZ917abc", "Luna",
                    Map.of("spotify", List.of(
                            new ExternalLink("https://open.spotify.com/user/spotify:artist:2AACqFGo8offvHCKGvrWxq", null))));

            assertThat(mapper.extractSpotifyArtistId(attraction)).isEqualTo("2AACqFGo8offvHCKGvrWxq");
        }

        @Test
        void extractSpotifyArtistId_givenUserProfileUrl_returnsNull() {
            TicketmasterAttractionResponse attraction = new TicketmasterAttractionResponse(
                    "K8vZ917def", "Black Joe Lewis",
                    Map.of("spotify", List.of(
                            new ExternalLink("https://open.spotify.com/user/blackjoelewismusic", null))));

            assertThat(mapper.extractSpotifyArtistId(attraction)).isNull();
        }

        @Test
        void extractSpotifyArtistId_givenPlaylistUrl_returnsNull() {
            TicketmasterAttractionResponse attraction = new TicketmasterAttractionResponse(
                    "K8vZ917ghi", "Emo Night",
                    Map.of("spotify", List.of(
                            new ExternalLink("https://open.spotify.com/playlist/3vwjBackAZ0Rl9hueMkOwp", null))));

            assertThat(mapper.extractSpotifyArtistId(attraction)).isNull();
        }

        @Test
        void extractSpotifyArtistId_givenAppleMusicUrl_returnsNull() {
            TicketmasterAttractionResponse attraction = new TicketmasterAttractionResponse(
                    "K8vZ917jkl", "Microwave",
                    Map.of("spotify", List.of(
                            new ExternalLink("https://music.apple.com/us/artist/microwave/613522668", null))));

            assertThat(mapper.extractSpotifyArtistId(attraction)).isNull();
        }

        @Test
        void extractSpotifyArtistId_givenUnrelatedUrl_returnsNull() {
            TicketmasterAttractionResponse attraction = new TicketmasterAttractionResponse(
                    "K8vZ917mno", "Trap Karaoke",
                    Map.of("spotify", List.of(
                            new ExternalLink("https://trapkaraoke.com/", null))));

            assertThat(mapper.extractSpotifyArtistId(attraction)).isNull();
        }

        @Test
        void extractSpotifyArtistId_givenNoExternalLinks_returnsNull() {
            TicketmasterAttractionResponse attraction = new TicketmasterAttractionResponse(
                    "K8vZ9171ob7", "Eagles", null);

            assertThat(mapper.extractSpotifyArtistId(attraction)).isNull();
        }

        @Test
        void extractSpotifyArtistId_givenNoSpotifyLink_returnsNull() {
            TicketmasterAttractionResponse attraction = new TicketmasterAttractionResponse(
                    "K8vZ9171ob7", "Eagles",
                    Map.of("youtube", List.of(
                            new ExternalLink("https://youtube.com/test", null))));

            assertThat(mapper.extractSpotifyArtistId(attraction)).isNull();
        }

        @Test
        void extractSpotifyArtistId_givenUrlWithQueryParams_extractsCleanId() {
            TicketmasterAttractionResponse attraction = new TicketmasterAttractionResponse(
                    "K8vZ917pqr", "Taylor Swift",
                    Map.of("spotify", List.of(
                            new ExternalLink("https://open.spotify.com/artist/06HL4z0CvFAxyc27GXpf02?si=abc123", null))));

            assertThat(mapper.extractSpotifyArtistId(attraction)).isEqualTo("06HL4z0CvFAxyc27GXpf02");
        }

        @Test
        void extractArtistIdFromUrl_givenNullUrl_returnsNull() {
            assertThat(mapper.extractArtistIdFromUrl(null)).isNull();
        }
    }

    // --- Image selection ---

    @Test
    void selectBestImage_givenMultipleImages_returnsLargest16x9() {
        List<Image> images = List.of(
                new Image("https://example.com/small.jpg", "16_9", 640, 360, false),
                new Image("https://example.com/large.jpg", "16_9", 2048, 1152, false),
                new Image("https://example.com/medium.jpg", "3_2", 1024, 683, false));

        assertThat(mapper.selectBestImage(images)).isEqualTo("https://example.com/large.jpg");
    }

    @Test
    void selectBestImage_givenNo16x9_returnsFallback() {
        List<Image> images = List.of(
                new Image("https://example.com/portrait.jpg", "3_2", 640, 427, false));

        assertThat(mapper.selectBestImage(images)).isEqualTo("https://example.com/portrait.jpg");
    }

    @Test
    void selectBestImage_givenNullWidths_handlesGracefully() {
        List<Image> images = List.of(
                new Image("https://example.com/nowidth.jpg", "16_9", null, null, null),
                new Image("https://example.com/withwidth.jpg", "16_9", 1024, 576, false));

        assertThat(mapper.selectBestImage(images)).isEqualTo("https://example.com/withwidth.jpg");
    }

    @Test
    void selectBestImage_givenNullImages_returnsNull() {
        assertThat(mapper.selectBestImage(null)).isNull();
    }

    // --- Price extraction ---

    @Test
    void extractBasePrice_givenPriceRanges_returnsMin() {
        List<PriceRange> ranges = List.of(
                new PriceRange("standard", "USD", 75.0, 250.0));

        assertThat(mapper.extractBasePrice(ranges)).isEqualByComparingTo(new BigDecimal("75.00"));
    }

    @Test
    void extractBasePrice_givenEmptyPriceRanges_returnsDefault() {
        assertThat(mapper.extractBasePrice(List.of())).isEqualByComparingTo(new BigDecimal("50.00"));
    }

    @Test
    void extractBasePrice_givenNullMin_returnsDefault() {
        List<PriceRange> ranges = List.of(
                new PriceRange("standard", "USD", null, 250.0));

        assertThat(mapper.extractBasePrice(ranges)).isEqualByComparingTo(new BigDecimal("50.00"));
    }

    @Test
    void extractBasePrice_givenNullPriceRanges_returnsDefault() {
        assertThat(mapper.extractBasePrice(null)).isEqualByComparingTo(new BigDecimal("50.00"));
    }

    // --- Date parsing ---

    @Test
    void parseEventDate_givenUtcDateTime_returnsInstant() {
        Dates dates = new Dates(
                new Start("2026-04-10", "20:30:00", "2026-04-11T03:30:00Z", false, false),
                "America/Los_Angeles",
                new Status("onsale"));

        assertThat(mapper.parseEventDate(dates)).isEqualTo(Instant.parse("2026-04-11T03:30:00Z"));
    }

    @Test
    void parseEventDate_givenNoUtcDateTime_parsesFromLocalDateAndTimezone() {
        Dates dates = new Dates(
                new Start("2026-04-10", "20:30:00", null, false, false),
                "America/Los_Angeles",
                new Status("onsale"));

        assertThat(mapper.parseEventDate(dates)).isNotNull();
    }

    // --- Doors times parsing ---

    @Nested
    class DoorsTimesParsing {

        @Test
        void parseDoorsTimes_givenUtcDateTime_returnsInstant() {
            DoorsTimes doorsTimes = new DoorsTimes("2026-04-10", "18:30:00", "2026-04-11T01:30:00Z");

            Instant result = mapper.parseDoorsTimes(doorsTimes, null);

            assertThat(result).isEqualTo(Instant.parse("2026-04-11T01:30:00Z"));
        }

        @Test
        void parseDoorsTimes_givenLocalOnly_usesEventTimezone() {
            DoorsTimes doorsTimes = new DoorsTimes("2026-04-10", "18:30:00", null);
            Dates dates = new Dates(null, "America/Los_Angeles", null);

            Instant result = mapper.parseDoorsTimes(doorsTimes, dates);

            assertThat(result).isNotNull();
        }

        @Test
        void parseDoorsTimes_givenNull_returnsNull() {
            assertThat(mapper.parseDoorsTimes(null, null)).isNull();
        }

        @Test
        void parseDoorsTimes_givenNullLocalFields_returnsNull() {
            DoorsTimes doorsTimes = new DoorsTimes(null, null, null);

            assertThat(mapper.parseDoorsTimes(doorsTimes, null)).isNull();
        }

        @Test
        void parseDoorsTimes_givenMalformedDateTime_returnsNullInsteadOfThrowing() {
            DoorsTimes doorsTimes = new DoorsTimes("not-a-date", "bad-time", "garbage");

            assertThat(mapper.parseDoorsTimes(doorsTimes, null)).isNull();
        }
    }

    // --- Event mapping ---

    @Test
    void mapToEvent_givenFullResponse_createsEventEntity() {
        TicketmasterEventResponse response = createSampleEventResponse();
        Venue venue = createSampleVenue();
        Category category = createSampleCategory();

        Event event = mapper.mapToEvent(response, venue, category);

        assertThat(event.getName()).isEqualTo("Eagles Live at Sphere");
        assertThat(event.getTicketmasterEventId()).isEqualTo("1A9ZkoaGkePdD04");
        assertThat(event.getVenue()).isEqualTo(venue);
        assertThat(event.getCategory()).isEqualTo(category);
        assertThat(event.getStatus()).isEqualTo("ACTIVE");
        assertThat(event.getSpotifyArtistId()).isEqualTo("0ECwFtbIWEVNwjlrfc6xoL");
        assertThat(event.getEventDate()).isNotNull();
        assertThat(event.getDoorsOpenAt()).isBefore(event.getEventDate());
        assertThat(event.getBasePrice()).isEqualByComparingTo(new BigDecimal("75.00"));
        assertThat(event.getMinPrice()).isEqualByComparingTo(new BigDecimal("60.00"));
        assertThat(event.getMaxPrice()).isEqualByComparingTo(new BigDecimal("187.50"));
        assertThat(event.isFeatured()).isTrue();
        assertThat(event.getSlug()).startsWith("eagles-live-at-sphere");
        assertThat(event.getPrimaryImageUrl()).contains("example.com");
    }

    @Test
    void mapToEvent_givenDoorsTimes_usesExplicitDoorsOpen() {
        TicketmasterEventResponse response = createEventResponseWithDoorsTimes();
        Venue venue = createSampleVenue();
        Category category = createSampleCategory();

        Event event = mapper.mapToEvent(response, venue, category);

        // Doors time is 18:30 UTC, event is 20:00 UTC — doors should be before event
        assertThat(event.getDoorsOpenAt()).isBefore(event.getEventDate());
        // Should use the explicit doors time, not event - 1 hour
        assertThat(event.getDoorsOpenAt()).isEqualTo(Instant.parse("2026-04-10T22:30:00Z"));
    }

    @Test
    void mapToEvent_givenNoDoorsTimes_defaultsToOneHourBefore() {
        TicketmasterEventResponse response = createSampleEventResponse();
        Venue venue = createSampleVenue();
        Category category = createSampleCategory();

        Event event = mapper.mapToEvent(response, venue, category);

        Instant eventDate = event.getEventDate();
        assertThat(event.getDoorsOpenAt()).isEqualTo(eventDate.minusSeconds(3600));
    }

    @Test
    void mapToEvent_givenCancelledStatus_setsStatusCancelled() {
        TicketmasterEventResponse response = createEventResponseWithStatus("cancelled");
        Venue venue = createSampleVenue();
        Category category = createSampleCategory();

        Event event = mapper.mapToEvent(response, venue, category);

        assertThat(event.getStatus()).isEqualTo("CANCELLED");
    }

    @Test
    void mapToEvent_givenNoAttractions_setsNullArtistAndSpotifyId() {
        TicketmasterEventResponse response = TicketmasterEventResponse.builder()
                .id("TM-NO-ARTIST")
                .name("Festival Event")
                .dates(new Dates(
                        new Start("2026-08-01", "18:00:00", "2026-08-01T22:00:00Z", false, false),
                        "America/New_York", new Status("onsale")))
                .classifications(List.of(new Classification(true,
                        new Segment("1", "Music"), new Genre("1", "Rock"), new SubGenre("1", "Pop"))))
                .embedded(new TicketmasterEventResponse.Embedded(null, null))
                .build();
        Venue venue = createSampleVenue();
        Category category = createSampleCategory();

        Event event = mapper.mapToEvent(response, venue, category);

        assertThat(event.getArtistName()).isNull();
        assertThat(event.getSpotifyArtistId()).isNull();
    }

    // --- Venue mapping ---

    @Test
    void mapToVenue_givenVenueResponse_createsVenueEntity() {
        TicketmasterVenueResponse venueResponse = createSampleVenueResponse();

        Venue venue = mapper.mapToVenue(venueResponse);

        assertThat(venue.getName()).isEqualTo("Sphere");
        assertThat(venue.getTicketmasterVenueId()).isEqualTo("KovZ917Atbr");
        assertThat(venue.getCity()).isEqualTo("Las Vegas");
        assertThat(venue.getState()).isEqualTo("NV");
        assertThat(venue.getCountry()).isEqualTo("US");
        assertThat(venue.getAddressLine1()).isEqualTo("255 Sands Avenue");
        assertThat(venue.getZipCode()).isEqualTo("89169");
        assertThat(venue.getLatitude()).isEqualByComparingTo(new BigDecimal("36.120727"));
        assertThat(venue.getLongitude()).isEqualByComparingTo(new BigDecimal("-115.164290"));
        assertThat(venue.getVenueType()).isEqualTo("ARENA");
        assertThat(venue.getCapacity()).isEqualTo(1000);
        assertThat(venue.getSlug()).startsWith("sphere");
    }

    @Test
    void mapToVenue_givenNullStateAndCity_usesDefaults() {
        TicketmasterVenueResponse venueResponse = new TicketmasterVenueResponse(
                "VENUE-INTL", "SunBet Arena",
                null, null, null, null,
                new TicketmasterVenueResponse.Country("South Africa", "ZA"),
                null);

        Venue venue = mapper.mapToVenue(venueResponse);

        assertThat(venue.getState()).isEqualTo("N/A");
        assertThat(venue.getCity()).isEqualTo("Unknown");
        assertThat(venue.getCountry()).isEqualTo("ZA");
    }

    @Test
    void mapToVenue_givenNullCountry_defaultsToUS() {
        TicketmasterVenueResponse venueResponse = new TicketmasterVenueResponse(
                "VENUE-NULL", "Test Venue",
                new TicketmasterVenueResponse.Address("123 St"),
                new TicketmasterVenueResponse.City("Test City"),
                new TicketmasterVenueResponse.State("Test", "TS"),
                "12345", null, null);

        Venue venue = mapper.mapToVenue(venueResponse);

        assertThat(venue.getCountry()).isEqualTo("US");
    }

    // --- Helper methods ---

    private TicketmasterEventResponse createSampleEventResponse() {
        return TicketmasterEventResponse.builder()
                .id("1A9ZkoaGkePdD04")
                .name("Eagles Live at Sphere")
                .url("https://www.ticketmaster.com/eagles-live-at-sphere")
                .dates(new Dates(
                        new Start("2026-04-10", "20:30:00", "2026-04-11T03:30:00Z", false, false),
                        "America/Los_Angeles",
                        new Status("onsale")))
                .classifications(List.of(new Classification(true,
                        new Segment("KZFzniwnSyZfZ7v7nJ", "Music"),
                        new Genre("1", "Rock"),
                        new SubGenre("1", "Pop"))))
                .images(List.of(new Image("https://example.com/large.jpg", "16_9", 2048, 1152, false)))
                .priceRanges(List.of(new PriceRange("standard", "USD", 75.0, 250.0)))
                .embedded(new Embedded(
                        List.of(createSampleVenueResponse()),
                        List.of(new TicketmasterAttractionResponse(
                                "K8vZ9171ob7", "Eagles",
                                Map.of("spotify", List.of(
                                        new ExternalLink("https://open.spotify.com/artist/0ECwFtbIWEVNwjlrfc6xoL", null)))))))
                .build();
    }

    private TicketmasterEventResponse createEventResponseWithDoorsTimes() {
        return TicketmasterEventResponse.builder()
                .id("TM-DOORS-001")
                .name("Concert With Doors Time")
                .dates(new Dates(
                        new Start("2026-04-10", "20:00:00", "2026-04-11T00:00:00Z", false, false),
                        "America/New_York",
                        new Status("onsale")))
                .classifications(List.of(new Classification(true,
                        new Segment("1", "Music"), new Genre("1", "Rock"), new SubGenre("1", "Pop"))))
                .doorsTimes(new DoorsTimes("2026-04-10", "18:30:00", "2026-04-10T22:30:00Z"))
                .embedded(new Embedded(null, null))
                .build();
    }

    private TicketmasterEventResponse createEventResponseWithStatus(String statusCode) {
        return TicketmasterEventResponse.builder()
                .id("1A9ZkoaGkePdD04")
                .name("Eagles Live at Sphere")
                .dates(new Dates(
                        new Start("2026-04-10", "20:30:00", "2026-04-11T03:30:00Z", false, false),
                        "America/Los_Angeles",
                        new Status(statusCode)))
                .classifications(List.of(new Classification(true,
                        new Segment("KZFzniwnSyZfZ7v7nJ", "Music"),
                        new Genre("1", "Rock"),
                        new SubGenre("1", "Pop"))))
                .build();
    }

    private TicketmasterVenueResponse createSampleVenueResponse() {
        return new TicketmasterVenueResponse(
                "KovZ917Atbr",
                "Sphere",
                new TicketmasterVenueResponse.Address("255 Sands Avenue"),
                new TicketmasterVenueResponse.City("Las Vegas"),
                new TicketmasterVenueResponse.State("Nevada", "NV"),
                "89169",
                new TicketmasterVenueResponse.Country("United States Of America", "US"),
                new TicketmasterVenueResponse.Location("36.12072670", "-115.16428960"));
    }

    private Venue createSampleVenue() {
        Venue venue = new Venue();
        venue.setName("Sphere");
        venue.setSlug("sphere");
        venue.setCity("Las Vegas");
        venue.setState("NV");
        venue.setCapacity(1000);
        venue.setVenueType("ARENA");
        venue.setAddressLine1("255 Sands Avenue");
        venue.setZipCode("89169");
        venue.setCountry("US");
        return venue;
    }

    private Category createSampleCategory() {
        Category category = new Category();
        category.setName("Concerts");
        category.setSlug("concerts");
        return category;
    }
}
