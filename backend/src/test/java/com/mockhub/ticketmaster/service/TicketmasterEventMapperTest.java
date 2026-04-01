package com.mockhub.ticketmaster.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.mockhub.event.entity.Category;
import com.mockhub.ticketmaster.dto.TicketmasterAttractionResponse;
import com.mockhub.ticketmaster.dto.TicketmasterEventResponse;
import com.mockhub.ticketmaster.dto.TicketmasterEventResponse.Classification;
import com.mockhub.ticketmaster.dto.TicketmasterEventResponse.Dates;
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

    @Test
    void resolveCategory_givenMusicSegment_returnsConcerts() {
        List<Classification> classifications = List.of(
                new Classification(true,
                        new Segment("KZFzniwnSyZfZ7v7nJ", "Music"),
                        new Genre("1", "Rock"),
                        new SubGenre("1", "Pop")));

        String slug = mapper.resolveCategorySlug(classifications);

        assertThat(slug).isEqualTo("concerts");
    }

    @Test
    void resolveCategory_givenSportsSegment_returnsSports() {
        List<Classification> classifications = List.of(
                new Classification(true,
                        new Segment("KZFzniwnSyZfZ7v7nE", "Sports"),
                        new Genre("1", "Basketball"),
                        new SubGenre("1", "NBA")));

        String slug = mapper.resolveCategorySlug(classifications);

        assertThat(slug).isEqualTo("sports");
    }

    @Test
    void resolveCategory_givenArtsAndTheatreSegment_returnsTheater() {
        List<Classification> classifications = List.of(
                new Classification(true,
                        new Segment("KZFzniwnSyZfZ7v7na", "Arts & Theatre"),
                        new Genre("1", "Theatre"),
                        new SubGenre("1", "Musical")));

        String slug = mapper.resolveCategorySlug(classifications);

        assertThat(slug).isEqualTo("theater");
    }

    @Test
    void resolveCategory_givenComedyGenreInArtsSegment_returnsComedy() {
        List<Classification> classifications = List.of(
                new Classification(true,
                        new Segment("KZFzniwnSyZfZ7v7na", "Arts & Theatre"),
                        new Genre("1", "Comedy"),
                        new SubGenre("1", "Stand-Up")));

        String slug = mapper.resolveCategorySlug(classifications);

        assertThat(slug).isEqualTo("comedy");
    }

    @Test
    void resolveCategory_givenNullClassifications_returnsOther() {
        String slug = mapper.resolveCategorySlug(null);

        assertThat(slug).isEqualTo("other");
    }

    @Test
    void resolveCategory_givenEmptyClassifications_returnsOther() {
        String slug = mapper.resolveCategorySlug(List.of());

        assertThat(slug).isEqualTo("other");
    }

    @Test
    void extractSpotifyArtistId_givenSpotifyUrl_returnsArtistId() {
        TicketmasterAttractionResponse attraction = new TicketmasterAttractionResponse(
                "K8vZ9171ob7", "Eagles",
                Map.of("spotify", List.of(
                        new TicketmasterAttractionResponse.ExternalLink(
                                "https://open.spotify.com/artist/0ECwFtbIWEVNwjlrfc6xoL", null))));

        String spotifyId = mapper.extractSpotifyArtistId(attraction);

        assertThat(spotifyId).isEqualTo("0ECwFtbIWEVNwjlrfc6xoL");
    }

    @Test
    void extractSpotifyArtistId_givenNoExternalLinks_returnsNull() {
        TicketmasterAttractionResponse attraction = new TicketmasterAttractionResponse(
                "K8vZ9171ob7", "Eagles", null);

        String spotifyId = mapper.extractSpotifyArtistId(attraction);

        assertThat(spotifyId).isNull();
    }

    @Test
    void extractSpotifyArtistId_givenNoSpotifyLink_returnsNull() {
        TicketmasterAttractionResponse attraction = new TicketmasterAttractionResponse(
                "K8vZ9171ob7", "Eagles",
                Map.of("youtube", List.of(
                        new TicketmasterAttractionResponse.ExternalLink(
                                "https://youtube.com/test", null))));

        String spotifyId = mapper.extractSpotifyArtistId(attraction);

        assertThat(spotifyId).isNull();
    }

    @Test
    void selectBestImage_givenMultipleImages_returnsLargest16x9() {
        List<Image> images = List.of(
                new Image("https://example.com/small.jpg", "16_9", 640, 360, false),
                new Image("https://example.com/large.jpg", "16_9", 2048, 1152, false),
                new Image("https://example.com/medium.jpg", "3_2", 1024, 683, false));

        String url = mapper.selectBestImage(images);

        assertThat(url).isEqualTo("https://example.com/large.jpg");
    }

    @Test
    void selectBestImage_givenNo16x9_returnsFallback() {
        List<Image> images = List.of(
                new Image("https://example.com/portrait.jpg", "3_2", 640, 427, false));

        String url = mapper.selectBestImage(images);

        assertThat(url).isEqualTo("https://example.com/portrait.jpg");
    }

    @Test
    void selectBestImage_givenNullImages_returnsNull() {
        String url = mapper.selectBestImage(null);

        assertThat(url).isNull();
    }

    @Test
    void extractBasePrice_givenPriceRanges_returnsMin() {
        List<PriceRange> ranges = List.of(
                new PriceRange("standard", "USD", 75.0, 250.0));

        BigDecimal price = mapper.extractBasePrice(ranges);

        assertThat(price).isEqualByComparingTo(new BigDecimal("75.00"));
    }

    @Test
    void extractBasePrice_givenEmptyPriceRanges_returnsDefault() {
        BigDecimal price = mapper.extractBasePrice(List.of());

        assertThat(price).isEqualByComparingTo(new BigDecimal("50.00"));
    }

    @Test
    void extractBasePrice_givenNullPriceRanges_returnsDefault() {
        BigDecimal price = mapper.extractBasePrice(null);

        assertThat(price).isEqualByComparingTo(new BigDecimal("50.00"));
    }

    @Test
    void parseEventDate_givenUtcDateTime_returnsInstant() {
        Dates dates = new Dates(
                new Start("2026-04-10", "20:30:00", "2026-04-11T03:30:00Z", false, false),
                "America/Los_Angeles",
                new Status("onsale"));

        Instant eventDate = mapper.parseEventDate(dates);

        assertThat(eventDate).isEqualTo(Instant.parse("2026-04-11T03:30:00Z"));
    }

    @Test
    void parseEventDate_givenNoUtcDateTime_parsesFromLocalDateAndTimezone() {
        Dates dates = new Dates(
                new Start("2026-04-10", "20:30:00", null, false, false),
                "America/Los_Angeles",
                new Status("onsale"));

        Instant eventDate = mapper.parseEventDate(dates);

        assertThat(eventDate).isNotNull();
    }

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
        assertThat(event.isFeatured()).isFalse();
        assertThat(event.getSlug()).startsWith("eagles-live-at-sphere");
        assertThat(event.getPrimaryImageUrl()).contains("example.com");
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

    // --- Helper methods ---

    private TicketmasterEventResponse createSampleEventResponse() {
        return new TicketmasterEventResponse(
                "1A9ZkoaGkePdD04",
                "Eagles Live at Sphere",
                "https://www.ticketmaster.com/eagles-live-at-sphere",
                new Dates(
                        new Start("2026-04-10", "20:30:00", "2026-04-11T03:30:00Z", false, false),
                        "America/Los_Angeles",
                        new Status("onsale")),
                List.of(new Classification(true,
                        new Segment("KZFzniwnSyZfZ7v7nJ", "Music"),
                        new Genre("1", "Rock"),
                        new SubGenre("1", "Pop"))),
                List.of(new Image("https://example.com/large.jpg", "16_9", 2048, 1152, false)),
                List.of(new PriceRange("standard", "USD", 75.0, 250.0)),
                new Embedded(
                        List.of(createSampleVenueResponse()),
                        List.of(new TicketmasterAttractionResponse(
                                "K8vZ9171ob7", "Eagles",
                                Map.of("spotify", List.of(
                                        new TicketmasterAttractionResponse.ExternalLink(
                                                "https://open.spotify.com/artist/0ECwFtbIWEVNwjlrfc6xoL", null)))))));
    }

    private TicketmasterEventResponse createEventResponseWithStatus(String statusCode) {
        return new TicketmasterEventResponse(
                "1A9ZkoaGkePdD04",
                "Eagles Live at Sphere",
                null,
                new Dates(
                        new Start("2026-04-10", "20:30:00", "2026-04-11T03:30:00Z", false, false),
                        "America/Los_Angeles",
                        new Status(statusCode)),
                List.of(new Classification(true,
                        new Segment("KZFzniwnSyZfZ7v7nJ", "Music"),
                        new Genre("1", "Rock"),
                        new SubGenre("1", "Pop"))),
                null, null, null);
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
