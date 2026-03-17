package com.mockhub.event.controller;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.mockhub.auth.security.JwtAuthenticationFilter;
import com.mockhub.auth.security.JwtTokenProvider;
import com.mockhub.auth.security.UserDetailsServiceImpl;
import com.mockhub.common.dto.PagedResponse;
import com.mockhub.common.exception.ResourceNotFoundException;
import com.mockhub.config.SecurityConfig;
import com.mockhub.event.dto.CategoryDto;
import com.mockhub.event.dto.EventDto;
import com.mockhub.event.dto.EventSummaryDto;
import com.mockhub.event.service.EventService;
import com.mockhub.pricing.service.PriceHistoryService;
import com.mockhub.ticket.service.ListingService;
import com.mockhub.ticket.service.TicketService;
import com.mockhub.venue.dto.VenueSummaryDto;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EventController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class EventControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EventService eventService;

    @MockitoBean
    private ListingService listingService;

    @MockitoBean
    private PriceHistoryService priceHistoryService;

    @MockitoBean
    private TicketService ticketService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private UserDetailsServiceImpl userDetailsService;

    private EventSummaryDto createTestSummary() {
        return new EventSummaryDto(
                1L, "Rock Festival", "rock-festival", "The Rockers",
                "Madison Square Garden", "New York",
                Instant.now().plus(30, ChronoUnit.DAYS),
                new BigDecimal("75.00"), 500, null, "Concert", true);
    }

    private EventDto createTestEventDto() {
        VenueSummaryDto venue = new VenueSummaryDto(
                1L, "Madison Square Garden", "madison-square-garden",
                "New York", "NY", "ARENA", 20000, null);
        CategoryDto category = new CategoryDto(1L, "Concert", "concert", "music", 1);

        return new EventDto(
                1L, "Rock Festival", "rock-festival", "A great event",
                "The Rockers", Instant.now().plus(30, ChronoUnit.DAYS), null,
                "ACTIVE", new BigDecimal("75.00"), new BigDecimal("75.00"),
                new BigDecimal("150.00"), 1000, 500, true,
                venue, category, List.of(), null);
    }

    @Test
    @DisplayName("GET /api/v1/events - returns paged event list")
    void listEvents_returnsPagedEventList() throws Exception {
        PagedResponse<EventSummaryDto> response = new PagedResponse<>(
                List.of(createTestSummary()), 0, 20, 1, 1);
        when(eventService.listEvents(any())).thenReturn(response);

        mockMvc.perform(get("/api/v1/events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].name").value("Rock Festival"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @DisplayName("GET /api/v1/events/featured - returns featured events")
    void listFeatured_returnsFeaturedEvents() throws Exception {
        when(eventService.listFeatured()).thenReturn(List.of(createTestSummary()));

        mockMvc.perform(get("/api/v1/events/featured"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].name").value("Rock Festival"))
                .andExpect(jsonPath("$[0].isFeatured").value(true));
    }

    @Test
    @DisplayName("GET /api/v1/events/{slug} - given existing slug - returns event detail")
    void getEvent_givenExistingSlug_returnsEventDetail() throws Exception {
        when(eventService.getBySlug("rock-festival")).thenReturn(createTestEventDto());

        mockMvc.perform(get("/api/v1/events/rock-festival"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Rock Festival"))
                .andExpect(jsonPath("$.slug").value("rock-festival"))
                .andExpect(jsonPath("$.venue.name").value("Madison Square Garden"));
    }

    @Test
    @DisplayName("GET /api/v1/events/{slug} - given nonexistent slug - returns 404")
    void getEvent_givenNonexistentSlug_returns404() throws Exception {
        when(eventService.getBySlug("nonexistent"))
                .thenThrow(new ResourceNotFoundException("Event", "slug", "nonexistent"));

        mockMvc.perform(get("/api/v1/events/nonexistent"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/v1/events/{slug}/listings - returns event listings")
    void getEventListings_returnsEventListings() throws Exception {
        when(listingService.getActiveListingsByEventSlug("rock-festival"))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/v1/events/rock-festival/listings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }
}
