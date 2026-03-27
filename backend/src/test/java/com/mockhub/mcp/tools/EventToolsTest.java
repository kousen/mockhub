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
import com.mockhub.ticket.dto.TicketSearchResultDto;
import com.mockhub.ticket.service.ListingService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
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
    @DisplayName("findTickets - given matching listings - returns JSON array")
    void findTickets_givenMatchingListings_returnsJsonArray() {
        TicketSearchResultDto result1 = createSearchResult(1L, "Rock Show", "rock-show", new BigDecimal("50.00"));
        TicketSearchResultDto result2 = createSearchResult(2L, "Rock Show", "rock-show", new BigDecimal("100.00"));
        when(listingService.searchTickets(eq("rock"), any(), any(), any(), any(), any(), any(), any(), eq(10)))
                .thenReturn(List.of(result1, result2));

        String result = eventTools.findTickets("rock", null, null, null, null, null, null, null, null);

        assertTrue(result.startsWith("["), "Result should be a JSON array");
        assertTrue(result.contains("\"listingId\":1"), "Result should contain first listing");
        assertTrue(result.contains("\"listingId\":2"), "Result should contain second listing");
        assertTrue(result.contains("\"eventName\":\"Rock Show\""), "Result should include event metadata");
    }

    @Test
    @DisplayName("findTickets - given filters - passes them to service")
    void findTickets_givenFilters_passesThemToService() {
        when(listingService.searchTickets(eq("jazz"), eq("jazz"), eq("LA"),
                eq(new BigDecimal("50.00")), eq(new BigDecimal("200.00")), eq("Orchestra"),
                any(), any(), eq(10)))
                .thenReturn(List.of());

        String result = eventTools.findTickets(
                "jazz", "jazz", "LA", null, null, new BigDecimal("50.00"), new BigDecimal("200.00"), "Orchestra", null);

        assertEquals("[]", result, "Result should be empty array when no matches");
    }

    @Test
    @DisplayName("findTickets - given ISO-8601 dates - parses and passes to service")
    void findTickets_givenIsoDates_parsesAndPassesToService() {
        Instant expectedFrom = Instant.parse("2026-04-01T00:00:00Z");
        Instant expectedTo = Instant.parse("2026-05-01T00:00:00Z");
        when(listingService.searchTickets(any(), any(), any(), any(), any(), any(),
                eq(expectedFrom), eq(expectedTo), eq(10)))
                .thenReturn(List.of());

        String result = eventTools.findTickets(
                null, null, null, "2026-04-01T00:00:00Z", "2026-05-01T00:00:00Z",
                null, null, null, null);

        assertEquals("[]", result);
        verify(listingService).searchTickets(any(), any(), any(), any(), any(), any(),
                eq(expectedFrom), eq(expectedTo), eq(10));
    }

    @Test
    @DisplayName("findTickets - given invalid date string - treats as null")
    void findTickets_givenInvalidDate_treatsAsNull() {
        when(listingService.searchTickets(any(), any(), any(), any(), any(), any(),
                isNull(), isNull(), eq(10)))
                .thenReturn(List.of());

        String result = eventTools.findTickets(
                null, null, null, "not-a-date", "also-not-a-date",
                null, null, null, null);

        assertEquals("[]", result);
    }

    @Test
    @DisplayName("findTickets - given maxResults - passes limit to service")
    void findTickets_givenMaxResults_passesLimitToService() {
        when(listingService.searchTickets(any(), any(), any(), any(), any(), any(), any(), any(), eq(5)))
                .thenReturn(List.of());

        eventTools.findTickets(null, null, null, null, null, null, null, null, 5);

        verify(listingService).searchTickets(any(), any(), any(), any(), any(), any(), any(), any(), eq(5));
    }

    @Test
    @DisplayName("findTickets - given no matching listings - returns empty array")
    void findTickets_givenNoMatchingListings_returnsEmptyArray() {
        when(listingService.searchTickets(eq("nonexistent"), any(), any(), any(), any(), any(), any(), any(), eq(10)))
                .thenReturn(List.of());

        String result = eventTools.findTickets("nonexistent", null, null, null, null, null, null, null, null);

        assertEquals("[]", result, "Result should be an empty JSON array");
    }

    @Test
    @DisplayName("findTickets - given service throws exception - returns error JSON")
    void findTickets_givenServiceThrowsException_returnsErrorJson() {
        when(listingService.searchTickets(any(), any(), any(), any(), any(), any(), any(), any(), any(int.class)))
                .thenThrow(new RuntimeException("Search failed"));

        String result = eventTools.findTickets("rock", null, null, null, null, null, null, null, null);

        assertTrue(result.contains("\"error\""), "Result should contain error field");
        assertTrue(result.contains("Failed to find tickets"), "Result should contain failure message");
    }

    @Test
    @DisplayName("findTickets - given maxResults over 50 - caps at 50")
    void findTickets_givenMaxResultsOver50_capsAt50() {
        when(listingService.searchTickets(any(), any(), any(), any(), any(), any(), any(), any(), eq(50)))
                .thenReturn(List.of());

        eventTools.findTickets(null, null, null, null, null, null, null, null, 100);

        verify(listingService).searchTickets(any(), any(), any(), any(), any(), any(), any(), any(), eq(50));
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

    private TicketSearchResultDto createSearchResult(Long listingId, String eventName,
                                                      String eventSlug, BigDecimal price) {
        return new TicketSearchResultDto(
                listingId, listingId, eventName, eventSlug, "Artist",
                "rock", "Venue", "NYC", Instant.now(),
                "Section A", "Row 1", "Seat " + listingId,
                "STANDARD", price, null);
    }
}
