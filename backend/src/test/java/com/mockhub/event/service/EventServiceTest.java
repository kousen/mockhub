package com.mockhub.event.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import com.mockhub.common.dto.PagedResponse;
import com.mockhub.common.exception.ResourceNotFoundException;
import com.mockhub.event.dto.EventCreateRequest;
import com.mockhub.event.dto.EventDto;
import com.mockhub.event.dto.EventSearchRequest;
import com.mockhub.event.dto.EventSummaryDto;
import com.mockhub.event.entity.Category;
import com.mockhub.event.entity.Event;
import com.mockhub.event.repository.CategoryRepository;
import com.mockhub.event.repository.EventRepository;
import com.mockhub.event.repository.TagRepository;
import com.mockhub.venue.entity.Venue;
import com.mockhub.venue.repository.VenueRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventServiceTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private TagRepository tagRepository;

    @Mock
    private VenueRepository venueRepository;

    @InjectMocks
    private EventService eventService;

    private Event testEvent;
    private Venue testVenue;
    private Category testCategory;

    @BeforeEach
    void setUp() {
        testVenue = new Venue();
        testVenue.setId(1L);
        testVenue.setName("Madison Square Garden");
        testVenue.setSlug("madison-square-garden");
        testVenue.setCity("New York");
        testVenue.setState("NY");
        testVenue.setVenueType("ARENA");
        testVenue.setCapacity(20000);

        testCategory = new Category();
        testCategory.setId(1L);
        testCategory.setName("Concert");
        testCategory.setSlug("concert");
        testCategory.setIcon("music");
        testCategory.setSortOrder(1);

        testEvent = new Event();
        testEvent.setId(1L);
        testEvent.setName("Rock Festival 2026");
        testEvent.setSlug("rock-festival-2026");
        testEvent.setDescription("An amazing rock festival");
        testEvent.setArtistName("The Rockers");
        testEvent.setEventDate(Instant.now().plus(30, ChronoUnit.DAYS));
        testEvent.setStatus("ACTIVE");
        testEvent.setBasePrice(new BigDecimal("75.00"));
        testEvent.setMinPrice(new BigDecimal("75.00"));
        testEvent.setMaxPrice(new BigDecimal("150.00"));
        testEvent.setTotalTickets(1000);
        testEvent.setAvailableTickets(500);
        testEvent.setFeatured(true);
        testEvent.setVenue(testVenue);
        testEvent.setCategory(testCategory);
        testEvent.setTags(new HashSet<>());
    }

    @Test
    @DisplayName("getBySlug - given existing slug - returns event DTO")
    void getBySlug_givenExistingSlug_returnsEventDto() {
        when(eventRepository.findBySlug("rock-festival-2026"))
                .thenReturn(Optional.of(testEvent));

        EventDto result = eventService.getBySlug("rock-festival-2026");

        assertNotNull(result, "Event DTO should not be null");
        assertEquals("Rock Festival 2026", result.name(), "Event name should match");
        assertEquals("rock-festival-2026", result.slug(), "Event slug should match");
        assertEquals("The Rockers", result.artistName(), "Artist name should match");
    }

    @Test
    @DisplayName("getBySlug - given nonexistent slug - throws ResourceNotFoundException")
    void getBySlug_givenNonexistentSlug_throwsResourceNotFoundException() {
        when(eventRepository.findBySlug("nonexistent")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> eventService.getBySlug("nonexistent"),
                "Should throw ResourceNotFoundException for unknown slug");
    }

    @SuppressWarnings("unchecked")
    @Test
    @DisplayName("listEvents - given default search request - returns paged response")
    void listEvents_givenDefaultSearchRequest_returnsPagedResponse() {
        EventSearchRequest request = new EventSearchRequest(
                null, null, null, null, null, null, null, null,
                null, null, null, null
        );

        Page<Event> page = new PageImpl<>(List.of(testEvent));
        when(eventRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(page);

        PagedResponse<EventSummaryDto> result = eventService.listEvents(request);

        assertNotNull(result, "Paged response should not be null");
        assertEquals(1, result.content().size(), "Should contain one event");
        assertEquals("Rock Festival 2026", result.content().get(0).name(),
                "Event name should match");
    }

    @Test
    @DisplayName("listFeatured - given featured events exist - returns list")
    void listFeatured_givenFeaturedEventsExist_returnsList() {
        when(eventRepository.findFeaturedEvents()).thenReturn(List.of(testEvent));

        List<EventSummaryDto> result = eventService.listFeatured();

        assertNotNull(result, "Featured list should not be null");
        assertEquals(1, result.size(), "Should contain one featured event");
        assertTrue(result.get(0).isFeatured(), "Event should be marked as featured");
    }

    @Test
    @DisplayName("createEvent - given valid request - returns created event DTO")
    void createEvent_givenValidRequest_returnsCreatedEventDto() {
        EventCreateRequest request = new EventCreateRequest(
                "New Concert", 1L, 1L,
                Instant.now().plus(60, ChronoUnit.DAYS),
                new BigDecimal("50.00"),
                "A new concert", "New Artist",
                null, null, false, null);

        when(venueRepository.findById(1L)).thenReturn(Optional.of(testVenue));
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> {
            Event event = invocation.getArgument(0);
            event.setId(2L);
            return event;
        });

        EventDto result = eventService.createEvent(request);

        assertNotNull(result, "Created event DTO should not be null");
        assertEquals("New Concert", result.name(), "Event name should match");
        verify(eventRepository).save(any(Event.class));
    }

    @Test
    @DisplayName("createEvent - given nonexistent venue - throws ResourceNotFoundException")
    void createEvent_givenNonexistentVenue_throwsResourceNotFoundException() {
        EventCreateRequest request = new EventCreateRequest(
                "New Concert", 999L, 1L,
                Instant.now().plus(60, ChronoUnit.DAYS),
                new BigDecimal("50.00"),
                null, null, null, null, null, null);

        when(venueRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> eventService.createEvent(request),
                "Should throw ResourceNotFoundException for unknown venue");
    }

    @Test
    @DisplayName("updateEvent - given existing event - updates and returns DTO")
    void updateEvent_givenExistingEvent_updatesAndReturnsDto() {
        EventCreateRequest request = new EventCreateRequest(
                "Updated Name", null, null, null,
                null, "Updated description", null,
                null, null, null, null);

        when(eventRepository.findById(1L)).thenReturn(Optional.of(testEvent));
        when(eventRepository.save(any(Event.class))).thenReturn(testEvent);

        EventDto result = eventService.updateEvent(1L, request);

        assertNotNull(result, "Updated event DTO should not be null");
        verify(eventRepository).save(any(Event.class));
    }

    @Test
    @DisplayName("updateEvent - given spotify artist ID - updates the field")
    void updateEvent_givenSpotifyArtistId_updatesTheField() {
        EventCreateRequest request = new EventCreateRequest(
                null, null, null, null,
                null, null, null,
                null, null, null, "06HL4z0CvFAxyc27GXpf02");

        when(eventRepository.findById(1L)).thenReturn(Optional.of(testEvent));
        when(eventRepository.save(any(Event.class))).thenReturn(testEvent);

        eventService.updateEvent(1L, request);

        verify(eventRepository).save(any(Event.class));
    }

    @Test
    @DisplayName("updateEvent - given nonexistent event - throws ResourceNotFoundException")
    void updateEvent_givenNonexistentEvent_throwsResourceNotFoundException() {
        EventCreateRequest request = new EventCreateRequest(
                "Updated", null, null, null, null,
                null, null, null, null, null, null);

        when(eventRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> eventService.updateEvent(999L, request),
                "Should throw ResourceNotFoundException for unknown event");
    }
}
