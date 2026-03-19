package com.mockhub.mcp.tools;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mockhub.common.dto.PagedResponse;
import com.mockhub.event.dto.EventDto;
import com.mockhub.event.dto.EventSearchRequest;
import com.mockhub.event.dto.EventSummaryDto;
import com.mockhub.event.service.EventService;
import com.mockhub.ticket.dto.ListingDto;
import com.mockhub.ticket.service.ListingService;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
    void getEventDetail_givenValidSlug_returnsEventJson() throws Exception {
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
    void getEventDetail_givenSlugWithWhitespace_stripsWhitespace() throws Exception {
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
    void getEventListings_givenValidSlug_returnsListingsJson() throws Exception {
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
}
