package com.mockhub.mcp.tools;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mockhub.common.dto.PagedResponse;
import com.mockhub.common.exception.ResourceNotFoundException;
import com.mockhub.event.dto.EventDto;
import com.mockhub.event.dto.EventSearchRequest;
import com.mockhub.event.dto.EventSummaryDto;
import com.mockhub.event.service.EventService;
import com.mockhub.ticket.dto.ListingDto;
import com.mockhub.ticket.service.ListingService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventToolsTest {

    @Mock
    private EventService eventService;

    @Mock
    private ListingService listingService;

    private ObjectMapper objectMapper;
    private EventTools eventTools;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
        eventTools = new EventTools(eventService, listingService, objectMapper);
    }

    // --- searchEvents ---

    @Test
    @DisplayName("searchEvents - given valid parameters - returns JSON with paged response")
    void searchEvents_givenValidParameters_returnsJsonWithPagedResponse() {
        PagedResponse<EventSummaryDto> pagedResponse = new PagedResponse<>(
                List.of(), 0, 20, 0, 0);
        when(eventService.listEvents(any(EventSearchRequest.class))).thenReturn(pagedResponse);

        String result = eventTools.searchEvents("rock", null, null, 0, 20);

        assertTrue(result.contains("\"content\""), "Result should contain content field");
        assertTrue(result.contains("\"totalElements\""), "Result should contain totalElements field");
    }

    @Test
    @DisplayName("searchEvents - given null parameters - defaults are applied and returns valid JSON")
    void searchEvents_givenNullParameters_defaultsAreAppliedAndReturnsValidJson() {
        PagedResponse<EventSummaryDto> pagedResponse = new PagedResponse<>(
                List.of(), 0, 20, 0, 0);
        when(eventService.listEvents(any(EventSearchRequest.class))).thenReturn(pagedResponse);

        String result = eventTools.searchEvents(null, null, null, null, null);

        assertTrue(result.contains("\"content\""), "Result should contain content field");
        verify(eventService).listEvents(any(EventSearchRequest.class));
    }

    @Test
    @DisplayName("searchEvents - given service throws exception - returns error JSON")
    void searchEvents_givenServiceThrowsException_returnsErrorJson() {
        when(eventService.listEvents(any(EventSearchRequest.class)))
                .thenThrow(new RuntimeException("Database connection failed"));

        String result = eventTools.searchEvents("rock", null, null, 0, 20);

        assertTrue(result.contains("\"error\""), "Result should contain error field");
        assertTrue(result.contains("Failed to search events"), "Result should contain failure message");
        assertTrue(result.contains("Database connection failed"), "Result should contain original error message");
    }

    // --- getEventDetail ---

    @Test
    @DisplayName("getEventDetail - given valid slug - returns event JSON")
    void getEventDetail_givenValidSlug_returnsEventJson() {
        EventDto eventDto = new EventDto(
                1L, "Rock Festival", "rock-festival", "A great show",
                null, null, null, null, null, null, null, 0, 0, false, null, null, null, null);
        when(eventService.getBySlug("rock-festival")).thenReturn(eventDto);

        String result = eventTools.getEventDetail("rock-festival");

        assertTrue(result.contains("\"name\":\"Rock Festival\""), "Result should contain event name");
        assertTrue(result.contains("\"slug\":\"rock-festival\""), "Result should contain event slug");
    }

    @Test
    @DisplayName("getEventDetail - given slug with whitespace - strips whitespace before lookup")
    void getEventDetail_givenSlugWithWhitespace_stripsWhitespace() {
        EventDto eventDto = new EventDto(
                1L, "Rock Festival", "rock-festival", "A great show",
                null, null, null, null, null, null, null, 0, 0, false, null, null, null, null);
        when(eventService.getBySlug("rock-festival")).thenReturn(eventDto);

        String result = eventTools.getEventDetail("  rock-festival  ");

        verify(eventService).getBySlug("rock-festival");
        assertTrue(result.contains("Rock Festival"), "Result should contain event name");
    }

    @Test
    @DisplayName("getEventDetail - given null slug - returns error JSON")
    void getEventDetail_givenNullSlug_returnsErrorJson() {
        String result = eventTools.getEventDetail(null);

        assertTrue(result.contains("\"error\""), "Result should contain error field");
        assertTrue(result.contains("Event slug is required"), "Result should indicate slug is required");
    }

    @Test
    @DisplayName("getEventDetail - given blank slug - returns error JSON")
    void getEventDetail_givenBlankSlug_returnsErrorJson() {
        String result = eventTools.getEventDetail("   ");

        assertTrue(result.contains("\"error\""), "Result should contain error field");
        assertTrue(result.contains("Event slug is required"), "Result should indicate slug is required");
    }

    @Test
    @DisplayName("getEventDetail - given service throws exception - returns error JSON")
    void getEventDetail_givenServiceThrowsException_returnsErrorJson() {
        when(eventService.getBySlug("nonexistent")).thenThrow(new RuntimeException("Event not found"));

        String result = eventTools.getEventDetail("nonexistent");

        assertTrue(result.contains("\"error\""), "Result should contain error field");
        assertTrue(result.contains("Failed to get event detail"), "Result should contain failure message");
    }

    // --- getEventListings ---

    @Test
    @DisplayName("getEventListings - given valid slug - returns listings JSON")
    void getEventListings_givenValidSlug_returnsListingsJson() {
        List<ListingDto> listings = List.of();
        when(listingService.getActiveListingsByEventSlug("rock-festival")).thenReturn(listings);

        String result = eventTools.getEventListings("rock-festival");

        assertTrue(result.startsWith("["), "Result should be a JSON array");
        verify(listingService).getActiveListingsByEventSlug("rock-festival");
    }

    @Test
    @DisplayName("getEventListings - given null slug - returns error JSON")
    void getEventListings_givenNullSlug_returnsErrorJson() {
        String result = eventTools.getEventListings(null);

        assertTrue(result.contains("\"error\""), "Result should contain error field");
        assertTrue(result.contains("Event slug is required"), "Result should indicate slug is required");
    }

    @Test
    @DisplayName("getEventListings - given blank slug - returns error JSON")
    void getEventListings_givenBlankSlug_returnsErrorJson() {
        String result = eventTools.getEventListings("");

        assertTrue(result.contains("\"error\""), "Result should contain error field");
    }

    @Test
    @DisplayName("getEventListings - given service throws exception - returns error JSON")
    void getEventListings_givenServiceThrowsException_returnsErrorJson() {
        when(listingService.getActiveListingsByEventSlug("bad-slug"))
                .thenThrow(new RuntimeException("Not found"));

        String result = eventTools.getEventListings("bad-slug");

        assertTrue(result.contains("\"error\""), "Result should contain error field");
        assertTrue(result.contains("Failed to get event listings"), "Result should contain failure message");
    }

    // --- getFeaturedEvents ---

    @Test
    @DisplayName("getFeaturedEvents - given featured events exist - returns JSON array")
    void getFeaturedEvents_givenFeaturedEventsExist_returnsJsonArray() {
        when(eventService.listFeatured()).thenReturn(List.of());

        String result = eventTools.getFeaturedEvents();

        assertTrue(result.startsWith("["), "Result should be a JSON array");
        verify(eventService).listFeatured();
    }

    @Test
    @DisplayName("getFeaturedEvents - given service throws exception - returns error JSON")
    void getFeaturedEvents_givenServiceThrowsException_returnsErrorJson() {
        when(eventService.listFeatured()).thenThrow(new RuntimeException("Service unavailable"));

        String result = eventTools.getFeaturedEvents();

        assertTrue(result.contains("\"error\""), "Result should contain error field");
        assertTrue(result.contains("Failed to get featured events"), "Result should contain failure message");
    }

    // --- getListingDetail ---

    @Test
    @DisplayName("getListingDetail - given valid listing ID - returns listing JSON")
    void getListingDetail_givenValidListingId_returnsListingJson() {
        ListingDto listingDto = createListingDto(1L, "event-1", new BigDecimal("75.00"));
        when(listingService.getListingById(1L)).thenReturn(listingDto);

        String result = eventTools.getListingDetail(1L);

        assertTrue(!result.contains("\"error\""), "Result should not contain error field");
        assertTrue(result.contains("\"id\":1"), "Result should contain listing ID");
        verify(listingService).getListingById(1L);
    }

    @Test
    @DisplayName("getListingDetail - given null listing ID - returns error JSON")
    void getListingDetail_givenNullListingId_returnsErrorJson() {
        String result = eventTools.getListingDetail(null);

        assertTrue(result.contains("\"error\""), "Result should contain error field");
        assertTrue(result.contains("Listing ID is required"), "Result should indicate listing ID is required");
    }

    @Test
    @DisplayName("getListingDetail - given nonexistent listing ID - returns error JSON")
    void getListingDetail_givenNonexistentListingId_returnsErrorJson() {
        when(listingService.getListingById(999L))
                .thenThrow(new ResourceNotFoundException("Listing", "id", 999L));

        String result = eventTools.getListingDetail(999L);

        assertTrue(result.contains("\"error\""), "Result should contain error field");
        assertTrue(result.contains("Failed to get listing detail"), "Result should contain failure message");
    }

    @Test
    @DisplayName("getListingDetail - given service throws exception - returns error JSON")
    void getListingDetail_givenServiceThrowsException_returnsErrorJson() {
        when(listingService.getListingById(1L)).thenThrow(new RuntimeException("Database error"));

        String result = eventTools.getListingDetail(1L);

        assertTrue(result.contains("\"error\""), "Result should contain error field");
        assertTrue(result.contains("Failed to get listing detail"), "Result should contain failure message");
    }

    // --- findTickets ---

    @Test
    @DisplayName("findTickets - given matching events and listings - returns sorted listings JSON")
    void findTickets_givenMatchingEventsAndListings_returnsSortedListingsJson() {
        EventSummaryDto event1 = new EventSummaryDto(
                1L, "Rock Show", "rock-show", "Band A", "Venue 1", "NYC",
                Instant.now(), new BigDecimal("50.00"), 10, null, "rock", false);
        PagedResponse<EventSummaryDto> eventsResponse = new PagedResponse<>(
                List.of(event1), 0, 100, 1, 1);
        when(eventService.listEvents(any(EventSearchRequest.class))).thenReturn(eventsResponse);

        ListingDto listing1 = createListingDto(1L, "rock-show", new BigDecimal("100.00"));
        ListingDto listing2 = createListingDto(2L, "rock-show", new BigDecimal("50.00"));
        when(listingService.getActiveListingsByEventSlug("rock-show"))
                .thenReturn(List.of(listing1, listing2));

        String result = eventTools.findTickets("rock", null, null, null, null, null, null, null, null);

        assertTrue(result.startsWith("["), "Result should be a JSON array");
        assertTrue(result.contains("\"listingId\":2"), "Result should contain cheaper listing");
        assertTrue(result.contains("\"listingId\":1"), "Result should contain both listings");
        assertTrue(result.contains("\"eventName\":\"Rock Show\""), "Result should include event metadata");
    }

    @Test
    @DisplayName("findTickets - given price filters - filters listings by price")
    void findTickets_givenPriceFilters_filtersListingsByPrice() {
        EventSummaryDto event1 = new EventSummaryDto(
                1L, "Jazz Night", "jazz-night", "Artist B", "Venue 2", "LA",
                Instant.now(), new BigDecimal("40.00"), 5, null, "jazz", false);
        PagedResponse<EventSummaryDto> eventsResponse = new PagedResponse<>(
                List.of(event1), 0, 100, 1, 1);
        when(eventService.listEvents(any(EventSearchRequest.class))).thenReturn(eventsResponse);

        ListingDto cheapListing = createListingDto(1L, "jazz-night", new BigDecimal("30.00"));
        ListingDto expensiveListing = createListingDto(2L, "jazz-night", new BigDecimal("200.00"));
        when(listingService.getActiveListingsByEventSlug("jazz-night"))
                .thenReturn(List.of(cheapListing, expensiveListing));

        String result = eventTools.findTickets(
                null, null, null, null, null, new BigDecimal("50.00"), new BigDecimal("250.00"), null, null);

        assertTrue(result.contains("\"listingId\":2"), "Result should contain listing within price range");
        assertTrue(!result.contains("\"listingId\":1"), "Result should not contain listing below min price");
    }

    @Test
    @DisplayName("findTickets - given section filter - filters listings by section")
    void findTickets_givenSectionFilter_filtersListingsBySection() {
        EventSummaryDto event1 = new EventSummaryDto(
                1L, "Pop Show", "pop-show", "Artist C", "Venue 3", "Chicago",
                Instant.now(), new BigDecimal("60.00"), 20, null, "pop", false);
        PagedResponse<EventSummaryDto> eventsResponse = new PagedResponse<>(
                List.of(event1), 0, 100, 1, 1);
        when(eventService.listEvents(any(EventSearchRequest.class))).thenReturn(eventsResponse);

        ListingDto orchestraListing = createListingDto(1L, "pop-show", new BigDecimal("100.00"), "Orchestra");
        ListingDto balconyListing = createListingDto(2L, "pop-show", new BigDecimal("50.00"), "Balcony");
        when(listingService.getActiveListingsByEventSlug("pop-show"))
                .thenReturn(List.of(orchestraListing, balconyListing));

        String result = eventTools.findTickets(null, null, null, null, null, null, null, "Orchestra", null);

        assertTrue(result.contains("\"listingId\":1"), "Result should contain Orchestra listing");
        assertTrue(!result.contains("\"listingId\":2"), "Result should not contain Balcony listing");
    }

    @Test
    @DisplayName("findTickets - given maxResults - limits number of results")
    void findTickets_givenMaxResults_limitsNumberOfResults() {
        EventSummaryDto event1 = new EventSummaryDto(
                1L, "Big Show", "big-show", "Artist D", "Venue 4", "Boston",
                Instant.now(), new BigDecimal("40.00"), 50, null, "rock", false);
        PagedResponse<EventSummaryDto> eventsResponse = new PagedResponse<>(
                List.of(event1), 0, 100, 1, 1);
        when(eventService.listEvents(any(EventSearchRequest.class))).thenReturn(eventsResponse);

        ListingDto listing1 = createListingDto(1L, "big-show", new BigDecimal("50.00"));
        ListingDto listing2 = createListingDto(2L, "big-show", new BigDecimal("60.00"));
        ListingDto listing3 = createListingDto(3L, "big-show", new BigDecimal("70.00"));
        when(listingService.getActiveListingsByEventSlug("big-show"))
                .thenReturn(List.of(listing1, listing2, listing3));

        String result = eventTools.findTickets(null, null, null, null, null, null, null, null, 2);

        assertTrue(result.contains("\"listingId\":1"), "Result should contain first listing");
        assertTrue(result.contains("\"listingId\":2"), "Result should contain second listing");
        assertTrue(!result.contains("\"listingId\":3"), "Result should not contain third listing (over limit)");
    }

    @Test
    @DisplayName("findTickets - given no matching events - returns empty array")
    void findTickets_givenNoMatchingEvents_returnsEmptyArray() {
        PagedResponse<EventSummaryDto> eventsResponse = new PagedResponse<>(
                List.of(), 0, 100, 0, 0);
        when(eventService.listEvents(any(EventSearchRequest.class))).thenReturn(eventsResponse);

        String result = eventTools.findTickets("nonexistent", null, null, null, null, null, null, null, null);

        assertEquals("[]", result, "Result should be an empty JSON array");
    }

    @Test
    @DisplayName("findTickets - given service throws exception - returns error JSON")
    void findTickets_givenServiceThrowsException_returnsErrorJson() {
        when(eventService.listEvents(any(EventSearchRequest.class)))
                .thenThrow(new RuntimeException("Search failed"));

        String result = eventTools.findTickets("rock", null, null, null, null, null, null, null, null);

        assertTrue(result.contains("\"error\""), "Result should contain error field");
        assertTrue(result.contains("Failed to find tickets"), "Result should contain failure message");
    }

    // --- Helper methods ---

    private ListingDto createListingDto(Long id, String eventSlug, BigDecimal price) {
        return createListingDto(id, eventSlug, price, "Section A");
    }

    private ListingDto createListingDto(Long id, String eventSlug, BigDecimal price, String section) {
        return new ListingDto(
                id, id, eventSlug, section, "Row 1", "Seat " + id,
                "STANDARD", price, price, BigDecimal.ONE,
                "ACTIVE", Instant.now(), null);
    }
}
