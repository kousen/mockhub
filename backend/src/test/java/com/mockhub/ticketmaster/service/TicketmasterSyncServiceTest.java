package com.mockhub.ticketmaster.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.mockhub.event.entity.Category;
import com.mockhub.event.entity.Event;
import com.mockhub.event.repository.CategoryRepository;
import com.mockhub.event.repository.EventRepository;
import com.mockhub.ticketmaster.dto.TicketmasterAttractionResponse;
import com.mockhub.ticketmaster.dto.TicketmasterAttractionResponse.ExternalLink;
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
import com.mockhub.venue.repository.VenueRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TicketmasterSyncServiceTest {

    @Mock
    private TicketmasterService ticketmasterService;
    @Mock
    private EventRepository eventRepository;
    @Mock
    private VenueRepository venueRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private TicketmasterTicketGenerator ticketGenerator;

    private TicketmasterSyncService syncService;

    @BeforeEach
    void setUp() {
        TicketmasterEventMapper eventMapper = new TicketmasterEventMapper();
        syncService = new TicketmasterSyncService(
                ticketmasterService, eventRepository, venueRepository,
                categoryRepository, eventMapper, ticketGenerator, 50);
    }

    @Test
    void processEvent_givenNewEvent_createsEventAndGeneratesTickets() {
        TicketmasterEventResponse tmEvent = createSampleEvent("TM-001", "Eagles Concert");
        Category category = createCategory("concerts");
        Venue venue = createVenue("Sphere", "Las Vegas");

        when(eventRepository.findByTicketmasterEventId("TM-001")).thenReturn(Optional.empty());
        when(venueRepository.findByTicketmasterVenueId("VENUE-001")).thenReturn(Optional.of(venue));
        when(categoryRepository.findBySlug("concerts")).thenReturn(Optional.of(category));
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TicketmasterSyncService.SyncResult result = syncService.processEvent(tmEvent);

        assertThat(result).isEqualTo(TicketmasterSyncService.SyncResult.NEW);
        verify(eventRepository, org.mockito.Mockito.times(2)).save(any(Event.class));
        verify(ticketGenerator).generateForEvent(any(Event.class));
    }

    @Test
    void processEvent_givenExistingEventUnchanged_skips() {
        TicketmasterEventResponse tmEvent = createSampleEvent("TM-001", "Eagles Concert");
        Event existing = new Event();
        existing.setTicketmasterEventId("TM-001");
        existing.setStatus("ACTIVE");
        existing.setEventDate(java.time.Instant.parse("2027-06-15T03:30:00Z"));
        existing.setPrimaryImageUrl("https://example.com/large.jpg");
        existing.setSpotifyArtistId("0ECwFtbIWEVNwjlrfc6xoL");
        existing.setArtistName("Eagles");

        when(eventRepository.findByTicketmasterEventId("TM-001")).thenReturn(Optional.of(existing));

        TicketmasterSyncService.SyncResult result = syncService.processEvent(tmEvent);

        assertThat(result).isEqualTo(TicketmasterSyncService.SyncResult.SKIPPED);
        verify(ticketGenerator, never()).generateForEvent(any());
    }

    @Test
    void processEvent_givenExistingEventMissingSpotify_backfills() {
        TicketmasterEventResponse tmEvent = createSampleEvent("TM-BACKFILL", "Eagles Concert");
        Event existing = new Event();
        existing.setTicketmasterEventId("TM-BACKFILL");
        existing.setStatus("ACTIVE");
        existing.setEventDate(java.time.Instant.parse("2027-06-15T03:30:00Z"));
        existing.setPrimaryImageUrl("https://example.com/large.jpg");
        existing.setSpotifyArtistId(null);
        existing.setArtistName(null);

        when(eventRepository.findByTicketmasterEventId("TM-BACKFILL")).thenReturn(Optional.of(existing));
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TicketmasterSyncService.SyncResult result = syncService.processEvent(tmEvent);

        assertThat(result).isEqualTo(TicketmasterSyncService.SyncResult.UPDATED);
        assertThat(existing.getSpotifyArtistId()).isEqualTo("0ECwFtbIWEVNwjlrfc6xoL");
        assertThat(existing.getArtistName()).isEqualTo("Eagles");
    }

    @Test
    void processEvent_givenExistingEventWithStatusChange_updates() {
        TicketmasterEventResponse tmEvent = createSampleEventWithStatus("TM-001", "cancelled");
        Event existing = new Event();
        existing.setTicketmasterEventId("TM-001");
        existing.setStatus("ACTIVE");
        existing.setEventDate(java.time.Instant.parse("2027-06-15T03:30:00Z"));

        when(eventRepository.findByTicketmasterEventId("TM-001")).thenReturn(Optional.of(existing));
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TicketmasterSyncService.SyncResult result = syncService.processEvent(tmEvent);

        assertThat(result).isEqualTo(TicketmasterSyncService.SyncResult.UPDATED);
        assertThat(existing.getStatus()).isEqualTo("CANCELLED");
    }

    @Test
    void processEvent_givenCancelledNewEvent_doesNotGenerateTickets() {
        TicketmasterEventResponse tmEvent = createSampleEventWithStatus("TM-CANCELLED", "cancelled");
        Venue venue = createVenue("Sphere", "Las Vegas");

        when(eventRepository.findByTicketmasterEventId("TM-CANCELLED")).thenReturn(Optional.empty());
        when(venueRepository.findByTicketmasterVenueId("VENUE-001")).thenReturn(Optional.of(venue));
        when(categoryRepository.findBySlug("other")).thenReturn(Optional.of(createCategory("other")));
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TicketmasterSyncService.SyncResult result = syncService.processEvent(tmEvent);

        assertThat(result).isEqualTo(TicketmasterSyncService.SyncResult.NEW);
        verify(eventRepository).save(any(Event.class));
        verify(ticketGenerator, never()).generateForEvent(any());
    }

    @Test
    void processEvent_givenNoDate_skips() {
        TicketmasterEventResponse tmEvent = TicketmasterEventResponse.builder()
                .id("TM-001")
                .name("No Date Event")
                .build();

        TicketmasterSyncService.SyncResult result = syncService.processEvent(tmEvent);

        assertThat(result).isEqualTo(TicketmasterSyncService.SyncResult.SKIPPED);
        verify(eventRepository, never()).save(any());
    }

    @Test
    void resolveVenue_givenExistingVenueByTmId_reuses() {
        TicketmasterEventResponse tmEvent = createSampleEvent("TM-001", "Test");
        Venue existing = createVenue("Sphere", "Las Vegas");

        when(venueRepository.findByTicketmasterVenueId("VENUE-001")).thenReturn(Optional.of(existing));

        Venue resolved = syncService.resolveVenue(tmEvent);

        assertThat(resolved).isEqualTo(existing);
        verify(venueRepository, never()).save(any());
    }

    @Test
    void resolveVenue_givenMatchByNameAndCity_linksExisting() {
        TicketmasterEventResponse tmEvent = createSampleEvent("TM-001", "Test");
        Venue existing = createVenue("Sphere", "Las Vegas");

        when(venueRepository.findByTicketmasterVenueId("VENUE-001")).thenReturn(Optional.empty());
        when(venueRepository.findByNameAndCity("Sphere", "Las Vegas")).thenReturn(Optional.of(existing));
        when(venueRepository.save(any(Venue.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Venue resolved = syncService.resolveVenue(tmEvent);

        assertThat(resolved.getTicketmasterVenueId()).isEqualTo("VENUE-001");
        verify(venueRepository).save(existing);
    }

    @Test
    void resolveVenue_givenNoMatch_createsNew() {
        TicketmasterEventResponse tmEvent = createSampleEvent("TM-001", "Test");

        when(venueRepository.findByTicketmasterVenueId("VENUE-001")).thenReturn(Optional.empty());
        when(venueRepository.findByNameAndCity("Sphere", "Las Vegas")).thenReturn(Optional.empty());
        when(venueRepository.save(any(Venue.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Venue resolved = syncService.resolveVenue(tmEvent);

        assertThat(resolved).isNotNull();
        assertThat(resolved.getName()).isEqualTo("Sphere");
        assertThat(resolved.getTicketmasterVenueId()).isEqualTo("VENUE-001");
        verify(venueRepository).save(any(Venue.class));
    }

    @Test
    void resolveVenue_givenNoEmbeddedVenues_returnsNull() {
        TicketmasterEventResponse tmEvent = TicketmasterEventResponse.builder()
                .id("TM-001")
                .name("Test")
                .dates(new Dates(new Start("2026-06-01", "20:00:00", "2026-06-01T20:00:00Z", false, false),
                        "America/New_York", new Status("onsale")))
                .build();

        Venue resolved = syncService.resolveVenue(tmEvent);

        assertThat(resolved).isNull();
    }

    @Test
    void syncEvents_givenEventsFromApi_processesAll() {
        TicketmasterEventResponse tmEvent = createSampleEvent("TM-SYNC-1", "Sync Test");
        Category category = createCategory("concerts");
        Venue venue = createVenue("Test Venue", "Test City");

        when(ticketmasterService.searchEvents(anyString(), anyString(), anyString(),
                org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.anyInt()))
                .thenReturn(List.of(tmEvent));
        when(eventRepository.findByTicketmasterEventId("TM-SYNC-1")).thenReturn(Optional.empty());
        when(venueRepository.findByTicketmasterVenueId("VENUE-001")).thenReturn(Optional.of(venue));
        when(categoryRepository.findBySlug("concerts")).thenReturn(Optional.of(category));
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> invocation.getArgument(0));

        syncService.syncEvents();

        verify(ticketmasterService, org.mockito.Mockito.times(3)).searchEvents(
                anyString(), anyString(), anyString(),
                org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.anyInt());
    }

    @Test
    void syncEvents_givenApiError_continuesWithOtherClassifications() {
        when(ticketmasterService.searchEvents(org.mockito.ArgumentMatchers.eq("music"),
                anyString(), anyString(), org.mockito.ArgumentMatchers.anyInt(),
                org.mockito.ArgumentMatchers.anyInt()))
                .thenThrow(new org.springframework.web.client.RestClientException("API error"));
        when(ticketmasterService.searchEvents(org.mockito.ArgumentMatchers.eq("sports"),
                anyString(), anyString(), org.mockito.ArgumentMatchers.anyInt(),
                org.mockito.ArgumentMatchers.anyInt()))
                .thenReturn(List.of());
        when(ticketmasterService.searchEvents(org.mockito.ArgumentMatchers.eq("arts & theatre"),
                anyString(), anyString(), org.mockito.ArgumentMatchers.anyInt(),
                org.mockito.ArgumentMatchers.anyInt()))
                .thenReturn(List.of());

        syncService.syncEvents();

        verify(ticketmasterService, org.mockito.Mockito.times(3)).searchEvents(
                anyString(), anyString(), anyString(),
                org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.anyInt());
    }

    @Test
    void processEvent_givenNullId_skips() {
        TicketmasterEventResponse tmEvent = TicketmasterEventResponse.builder()
                .name("No ID Event")
                .dates(new Dates(new Start("2026-06-01", "20:00:00", "2026-06-01T20:00:00Z", false, false),
                        "America/New_York", new Status("onsale")))
                .build();

        TicketmasterSyncService.SyncResult result = syncService.processEvent(tmEvent);

        assertThat(result).isEqualTo(TicketmasterSyncService.SyncResult.SKIPPED);
    }

    @Test
    void processEvent_givenNullName_skips() {
        TicketmasterEventResponse tmEvent = TicketmasterEventResponse.builder()
                .id("TM-001")
                .dates(new Dates(new Start("2026-06-01", "20:00:00", "2026-06-01T20:00:00Z", false, false),
                        "America/New_York", new Status("onsale")))
                .build();

        TicketmasterSyncService.SyncResult result = syncService.processEvent(tmEvent);

        assertThat(result).isEqualTo(TicketmasterSyncService.SyncResult.SKIPPED);
    }

    @Test
    void processEvent_givenNoVenue_skips() {
        TicketmasterEventResponse tmEvent = createSampleEvent("TM-NO-VENUE", "No Venue");
        // Override with no embedded venues
        TicketmasterEventResponse noVenue = TicketmasterEventResponse.builder()
                .id("TM-NO-VENUE")
                .name("No Venue")
                .dates(tmEvent.dates())
                .classifications(tmEvent.classifications())
                .images(tmEvent.images())
                .priceRanges(tmEvent.priceRanges())
                .build();

        when(eventRepository.findByTicketmasterEventId("TM-NO-VENUE")).thenReturn(Optional.empty());

        TicketmasterSyncService.SyncResult result = syncService.processEvent(noVenue);

        assertThat(result).isEqualTo(TicketmasterSyncService.SyncResult.SKIPPED);
    }

    @Test
    void processEvent_givenExistingEventWithDateChange_updates() {
        TicketmasterEventResponse tmEvent = createSampleEvent("TM-DATE", "Date Change Event");
        Event existing = new Event();
        existing.setTicketmasterEventId("TM-DATE");
        existing.setStatus("ACTIVE");
        existing.setEventDate(java.time.Instant.parse("2026-01-01T00:00:00Z"));
        existing.setPrimaryImageUrl("https://example.com/large.jpg");

        when(eventRepository.findByTicketmasterEventId("TM-DATE")).thenReturn(Optional.of(existing));
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TicketmasterSyncService.SyncResult result = syncService.processEvent(tmEvent);

        assertThat(result).isEqualTo(TicketmasterSyncService.SyncResult.UPDATED);
        assertThat(existing.getEventDate()).isEqualTo(java.time.Instant.parse("2027-06-15T03:30:00Z"));
    }

    @Test
    void backfillSpotifyIds_givenEventWithSpotifyAttraction_updatesSpotifyId() {
        Event event = new Event();
        event.setId(1L);
        event.setName("Eagles Concert");
        event.setTicketmasterEventId("TM-001");
        event.setSpotifyArtistId(null);

        TicketmasterEventResponse tmEvent = createSampleEvent("TM-001", "Eagles Concert");
        when(eventRepository.findMissingSpotifyWithTicketmasterId()).thenReturn(List.of(event));
        when(ticketmasterService.getEvent("TM-001")).thenReturn(tmEvent);
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> invocation.getArgument(0));

        int updated = syncService.backfillSpotifyIds();

        assertThat(updated).isEqualTo(1);
        assertThat(event.getSpotifyArtistId()).isEqualTo("0ECwFtbIWEVNwjlrfc6xoL");
        verify(eventRepository).save(event);
    }

    @Test
    void backfillSpotifyIds_givenEventWithNoAttractions_skips() {
        Event event = new Event();
        event.setId(1L);
        event.setName("Hamilton");
        event.setTicketmasterEventId("TM-HAMILTON");
        event.setSpotifyArtistId(null);

        TicketmasterEventResponse tmEvent = TicketmasterEventResponse.builder()
                .id("TM-HAMILTON")
                .name("Hamilton")
                .dates(new Dates(new Start("2026-06-01", "20:00:00", "2026-06-01T20:00:00Z", false, false),
                        "America/New_York", new Status("onsale")))
                .embedded(new TicketmasterEventResponse.Embedded(null, null))
                .build();

        when(eventRepository.findMissingSpotifyWithTicketmasterId()).thenReturn(List.of(event));
        when(ticketmasterService.getEvent("TM-HAMILTON")).thenReturn(tmEvent);

        int updated = syncService.backfillSpotifyIds();

        assertThat(updated).isEqualTo(0);
        verify(eventRepository, never()).save(any());
    }

    @Test
    void backfillSpotifyIds_givenApiReturnsNull_skips() {
        Event event = new Event();
        event.setId(1L);
        event.setName("Missing Event");
        event.setTicketmasterEventId("TM-GONE");
        event.setSpotifyArtistId(null);

        when(eventRepository.findMissingSpotifyWithTicketmasterId()).thenReturn(List.of(event));
        when(ticketmasterService.getEvent("TM-GONE")).thenReturn(null);

        int updated = syncService.backfillSpotifyIds();

        assertThat(updated).isEqualTo(0);
        verify(eventRepository, never()).save(any());
    }

    // --- Helper methods ---

    private TicketmasterEventResponse createSampleEvent(String id, String name) {
        return TicketmasterEventResponse.builder()
                .id(id)
                .name(name)
                .dates(new Dates(
                        new Start("2027-06-14", "20:30:00", "2027-06-15T03:30:00Z", false, false),
                        "America/Los_Angeles",
                        new Status("onsale")))
                .classifications(List.of(new Classification(true,
                        new Segment("KZFzniwnSyZfZ7v7nJ", "Music"),
                        new Genre("1", "Rock"),
                        new SubGenre("1", "Pop"))))
                .images(List.of(new Image("https://example.com/large.jpg", "16_9", 2048, 1152, false)))
                .priceRanges(List.of(new PriceRange("standard", "USD", 75.0, 250.0)))
                .embedded(new Embedded(
                        List.of(new TicketmasterVenueResponse(
                                "VENUE-001", "Sphere",
                                new TicketmasterVenueResponse.Address("255 Sands Ave"),
                                new TicketmasterVenueResponse.City("Las Vegas"),
                                new TicketmasterVenueResponse.State("Nevada", "NV"),
                                "89169",
                                new TicketmasterVenueResponse.Country("US", "US"),
                                new TicketmasterVenueResponse.Location("36.12", "-115.16"))),
                        List.of(new TicketmasterAttractionResponse(
                                "ATTR-001", "Eagles",
                                Map.of("spotify", List.of(
                                        new ExternalLink("https://open.spotify.com/artist/0ECwFtbIWEVNwjlrfc6xoL", null)))))))
                .build();
    }

    private TicketmasterEventResponse createSampleEventWithStatus(String id, String statusCode) {
        return TicketmasterEventResponse.builder()
                .id(id)
                .name("Test Event")
                .dates(new Dates(
                        new Start("2027-06-14", "20:30:00", "2027-06-15T03:30:00Z", false, false),
                        "America/Los_Angeles",
                        new Status(statusCode)))
                .embedded(new Embedded(
                        List.of(new TicketmasterVenueResponse(
                                "VENUE-001", "Sphere",
                                null, new TicketmasterVenueResponse.City("Las Vegas"),
                                new TicketmasterVenueResponse.State("NV", "NV"),
                                null, null, null)),
                        null))
                .build();
    }

    private Category createCategory(String slug) {
        Category category = new Category();
        category.setName(slug.substring(0, 1).toUpperCase() + slug.substring(1));
        category.setSlug(slug);
        return category;
    }

    private Venue createVenue(String name, String city) {
        Venue venue = new Venue();
        venue.setName(name);
        venue.setCity(city);
        venue.setCapacity(1000);
        venue.setVenueType("ARENA");
        venue.setSlug(name.toLowerCase().replace(" ", "-"));
        venue.setAddressLine1("123 Test St");
        venue.setState("NV");
        venue.setZipCode("89169");
        venue.setCountry("US");
        return venue;
    }
}
