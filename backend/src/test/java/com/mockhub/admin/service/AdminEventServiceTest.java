package com.mockhub.admin.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.mockhub.admin.dto.AdminEventDto;
import com.mockhub.common.dto.PagedResponse;
import com.mockhub.common.exception.ResourceNotFoundException;
import com.mockhub.event.dto.EventCreateRequest;
import com.mockhub.event.dto.EventDto;
import com.mockhub.event.entity.Category;
import com.mockhub.event.entity.Event;
import com.mockhub.event.repository.EventRepository;
import com.mockhub.event.service.EventService;
import com.mockhub.ticket.repository.TicketRepository;
import com.mockhub.venue.entity.Seat;
import com.mockhub.venue.entity.SeatRow;
import com.mockhub.venue.entity.Section;
import com.mockhub.venue.entity.Venue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminEventServiceTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private EventService eventService;

    @Mock
    private TicketRepository ticketRepository;

    @InjectMocks
    private AdminEventService adminEventService;

    private Event testEvent;

    @BeforeEach
    void setUp() {
        Venue testVenue = new Venue();
        testVenue.setId(1L);
        testVenue.setName("Test Venue");
        testVenue.setSlug("test-venue");
        testVenue.setCity("New York");
        testVenue.setState("NY");
        testVenue.setVenueType("ARENA");
        testVenue.setCapacity(20000);
        testVenue.setSections(new ArrayList<>());

        Category testCategory = new Category();
        testCategory.setId(1L);
        testCategory.setName("Concert");
        testCategory.setSlug("concert");
        testCategory.setIcon("music");
        testCategory.setSortOrder(1);

        testEvent = new Event();
        testEvent.setId(1L);
        testEvent.setName("Test Event");
        testEvent.setSlug("test-event");
        testEvent.setDescription("Description");
        testEvent.setArtistName("Artist");
        testEvent.setEventDate(Instant.now().plus(30, ChronoUnit.DAYS));
        testEvent.setStatus("ACTIVE");
        testEvent.setBasePrice(new BigDecimal("50.00"));
        testEvent.setMinPrice(new BigDecimal("50.00"));
        testEvent.setMaxPrice(new BigDecimal("100.00"));
        testEvent.setTotalTickets(1000);
        testEvent.setAvailableTickets(800);
        testEvent.setFeatured(false);
        testEvent.setVenue(testVenue);
        testEvent.setCategory(testCategory);
        testEvent.setTags(new HashSet<>());
    }

    @Test
    @DisplayName("getAllEvents - given events exist - returns paged admin event DTOs")
    void getAllEvents_givenEventsExist_returnsPagedAdminEventDtos() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<Event> page = new PageImpl<>(List.of(testEvent));
        when(eventRepository.findAll(pageable)).thenReturn(page);

        PagedResponse<AdminEventDto> result = adminEventService.getAllEvents(pageable);

        assertNotNull(result, "Paged response should not be null");
        assertEquals(1, result.content().size(), "Should contain one event");
        assertEquals("Test Event", result.content().get(0).name(), "Event name should match");
    }

    @Test
    @DisplayName("deleteEvent - given existing event - sets status to CANCELLED")
    void deleteEvent_givenExistingEvent_setsStatusToCancelled() {
        when(eventRepository.findById(1L)).thenReturn(Optional.of(testEvent));

        adminEventService.deleteEvent(1L);

        assertEquals("CANCELLED", testEvent.getStatus(), "Event status should be CANCELLED");
        verify(eventRepository).save(testEvent);
    }

    @Test
    @DisplayName("deleteEvent - given nonexistent event - throws ResourceNotFoundException")
    void deleteEvent_givenNonexistentEvent_throwsResourceNotFoundException() {
        when(eventRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> adminEventService.deleteEvent(999L),
                "Should throw ResourceNotFoundException for unknown event");
    }

    @Test
    @DisplayName("generateTicketsForEvent - given event with GA section - generates GA tickets")
    void generateTicketsForEvent_givenEventWithGaSection_generatesGaTickets() {
        Section gaSection = new Section();
        gaSection.setId(1L);
        gaSection.setSectionType("GENERAL_ADMISSION");
        gaSection.setCapacity(100);
        gaSection.setSeatRows(new ArrayList<>());

        testEvent.getVenue().setSections(List.of(gaSection));
        when(eventRepository.findById(1L)).thenReturn(Optional.of(testEvent));
        when(ticketRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        int result = adminEventService.generateTicketsForEvent(1L);

        assertEquals(100, result, "Should generate 100 GA tickets");
        verify(eventRepository).save(testEvent);
    }

    @Test
    @DisplayName("generateTicketsForEvent - given event with seated section - generates reserved tickets")
    void generateTicketsForEvent_givenEventWithSeatedSection_generatesReservedTickets() {
        Seat seat1 = new Seat();
        seat1.setId(1L);
        Seat seat2 = new Seat();
        seat2.setId(2L);

        SeatRow row = new SeatRow();
        row.setId(1L);
        row.setSeats(List.of(seat1, seat2));

        Section seatedSection = new Section();
        seatedSection.setId(1L);
        seatedSection.setSectionType("RESERVED");
        seatedSection.setSeatRows(List.of(row));

        testEvent.getVenue().setSections(List.of(seatedSection));
        when(eventRepository.findById(1L)).thenReturn(Optional.of(testEvent));
        when(ticketRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        int result = adminEventService.generateTicketsForEvent(1L);

        assertEquals(2, result, "Should generate 2 reserved tickets");
    }

    @Test
    @DisplayName("generateTicketsForEvent - given nonexistent event - throws ResourceNotFoundException")
    void generateTicketsForEvent_givenNonexistentEvent_throwsResourceNotFoundException() {
        when(eventRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> adminEventService.generateTicketsForEvent(999L));
    }

    @Test
    @DisplayName("getSpotifyStatusForEvents - given matching events - returns status list")
    void getSpotifyStatusForEvents_givenMatchingEvents_returnsStatusList() {
        testEvent.setSpotifyArtistId("0oSGxfWSnnOXhD2fKuz2Gy");
        testEvent.setTicketmasterEventId("TM-12345");
        when(eventRepository.findAll()).thenReturn(List.of(testEvent));

        List<Map<String, Object>> result = adminEventService.getSpotifyStatusForEvents("Test");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).get("name")).isEqualTo("Test Event");
        assertThat(result.get(0).get("spotifyArtistId")).isEqualTo("0oSGxfWSnnOXhD2fKuz2Gy");
    }

    @Test
    @DisplayName("activateTicketmasterEvents - deactivates seed and features TM events")
    void activateTicketmasterEvents_deactivatesSeedAndFeaturesTm() {
        when(eventRepository.deactivateSeedEvents()).thenReturn(100);
        when(eventRepository.featureTicketmasterEvents()).thenReturn(83);
        when(eventRepository.completePastTicketmasterEvents(any())).thenReturn(5);

        Map<String, Integer> result = adminEventService.activateTicketmasterEvents();

        assertThat(result.get("seedEventsDeactivated")).isEqualTo(100);
        assertThat(result.get("ticketmasterEventsFeatured")).isEqualTo(83);
        assertThat(result.get("pastEventsCompleted")).isEqualTo(5);
    }
}
