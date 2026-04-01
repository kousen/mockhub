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
        existing.setEventDate(java.time.Instant.parse("2026-04-11T03:30:00Z"));
        existing.setPrimaryImageUrl("https://example.com/large.jpg");

        when(eventRepository.findByTicketmasterEventId("TM-001")).thenReturn(Optional.of(existing));

        TicketmasterSyncService.SyncResult result = syncService.processEvent(tmEvent);

        assertThat(result).isEqualTo(TicketmasterSyncService.SyncResult.SKIPPED);
        verify(ticketGenerator, never()).generateForEvent(any());
    }

    @Test
    void processEvent_givenExistingEventWithStatusChange_updates() {
        TicketmasterEventResponse tmEvent = createSampleEventWithStatus("TM-001", "cancelled");
        Event existing = new Event();
        existing.setTicketmasterEventId("TM-001");
        existing.setStatus("ACTIVE");
        existing.setEventDate(java.time.Instant.parse("2026-04-11T03:30:00Z"));

        when(eventRepository.findByTicketmasterEventId("TM-001")).thenReturn(Optional.of(existing));
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TicketmasterSyncService.SyncResult result = syncService.processEvent(tmEvent);

        assertThat(result).isEqualTo(TicketmasterSyncService.SyncResult.UPDATED);
        assertThat(existing.getStatus()).isEqualTo("CANCELLED");
    }

    @Test
    void processEvent_givenCancelledNewEvent_doesNotGenerateTickets() {
        TicketmasterEventResponse tmEvent = createSampleEventWithStatus("TM-CANCELLED", "cancelled");
        Category category = createCategory("concerts");
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
        TicketmasterEventResponse tmEvent = new TicketmasterEventResponse(
                "TM-001", "No Date Event", null, null, null, null, null, null);

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
        TicketmasterEventResponse tmEvent = new TicketmasterEventResponse(
                "TM-001", "Test", null,
                new Dates(new Start("2026-06-01", "20:00:00", "2026-06-01T20:00:00Z", false, false),
                        "America/New_York", new Status("onsale")),
                null, null, null, null);

        Venue resolved = syncService.resolveVenue(tmEvent);

        assertThat(resolved).isNull();
    }

    // --- Helper methods ---

    private TicketmasterEventResponse createSampleEvent(String id, String name) {
        return new TicketmasterEventResponse(
                id, name, null,
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
                                        new TicketmasterAttractionResponse.ExternalLink(
                                                "https://open.spotify.com/artist/0ECwFtbIWEVNwjlrfc6xoL", null)))))));
    }

    private TicketmasterEventResponse createSampleEventWithStatus(String id, String statusCode) {
        return new TicketmasterEventResponse(
                id, "Test Event", null,
                new Dates(
                        new Start("2026-04-10", "20:30:00", "2026-04-11T03:30:00Z", false, false),
                        "America/Los_Angeles",
                        new Status(statusCode)),
                null, null, null,
                new Embedded(
                        List.of(new TicketmasterVenueResponse(
                                "VENUE-001", "Sphere",
                                null, new TicketmasterVenueResponse.City("Las Vegas"),
                                new TicketmasterVenueResponse.State("NV", "NV"),
                                null, null, null)),
                        null));
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
