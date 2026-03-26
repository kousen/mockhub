package com.mockhub.mcp.tools;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mockhub.common.dto.PagedResponse;
import com.mockhub.event.dto.EventDto;
import com.mockhub.event.dto.EventSearchRequest;
import com.mockhub.event.dto.EventSummaryDto;
import com.mockhub.event.service.EventService;
import com.mockhub.ticket.dto.ListingDto;
import com.mockhub.ticket.dto.TicketSearchResultDto;
import com.mockhub.ticket.service.ListingService;

@Component
public class EventTools {

    private static final Logger log = LoggerFactory.getLogger(EventTools.class);

    private final EventService eventService;
    private final ListingService listingService;
    private final ObjectMapper objectMapper;

    public EventTools(EventService eventService,
                      ListingService listingService,
                      ObjectMapper objectMapper) {
        this.eventService = eventService;
        this.listingService = listingService;
        this.objectMapper = objectMapper;
    }

    @Tool(description = "Search and filter events by query text, category, city, and pagination. "
            + "Returns a paginated list of event summaries with name, artist, venue, date, and price.")
    public String searchEvents(
            @ToolParam(description = "Search query text to match event name or artist",
                    required = false) String query,
            @ToolParam(description = "Category slug to filter by: 'concerts', 'sports', 'theater', 'comedy', 'festivals'",
                    required = false) String category,
            @ToolParam(description = "City name to filter events by location",
                    required = false) String city,
            @ToolParam(description = "Page number (0-based), defaults to 0",
                    required = false) Integer page,
            @ToolParam(description = "Page size (1-100), defaults to 20",
                    required = false) Integer size) {
        try {
            EventSearchRequest request = new EventSearchRequest(
                    query, category, null, city,
                    null, null, null, null,
                    "ACTIVE", "eventDate",
                    page, size);

            PagedResponse<EventSummaryDto> response = eventService.listEvents(request);
            return objectMapper.writeValueAsString(response);
        } catch (Exception e) {
            log.error("Error searching events: {}", e.getMessage(), e);
            return errorJson("Failed to search events: " + e.getMessage());
        }
    }

    @Tool(description = "Get full details for a specific event by its URL slug. "
            + "Returns event name, description, artist, venue, date, pricing, and ticket availability.")
    public String getEventDetail(
            @ToolParam(description = "Event URL slug (e.g. 'taylor-swift-eras-tour-nyc')",
                    required = true) String slug) {
        try {
            if (slug == null || slug.isBlank()) {
                return errorJson("Event slug is required");
            }
            EventDto event = eventService.getBySlug(slug.strip());
            return objectMapper.writeValueAsString(event);
        } catch (Exception e) {
            log.error("Error getting event detail for slug '{}': {}", slug, e.getMessage(), e);
            return errorJson("Failed to get event detail: " + e.getMessage());
        }
    }

    @Tool(description = "Get all active ticket listings for a specific event. "
            + "Returns listing details including section, row, seat, price, and status.")
    public String getEventListings(
            @ToolParam(description = "Event URL slug to get listings for", required = true) String slug) {
        try {
            if (slug == null || slug.isBlank()) {
                return errorJson("Event slug is required");
            }
            List<ListingDto> listings = listingService.getActiveListingsByEventSlug(slug.strip());
            return objectMapper.writeValueAsString(listings);
        } catch (Exception e) {
            log.error("Error getting listings for slug '{}': {}", slug, e.getMessage(), e);
            return errorJson("Failed to get event listings: " + e.getMessage());
        }
    }

    @Tool(description = "Get the list of featured/highlighted events on MockHub. "
            + "Returns a curated list of event summaries for prominent upcoming events.")
    public String getFeaturedEvents() {
        try {
            List<EventSummaryDto> featured = eventService.listFeatured();
            return objectMapper.writeValueAsString(featured);
        } catch (Exception e) {
            log.error("Error getting featured events: {}", e.getMessage(), e);
            return errorJson("Failed to get featured events: " + e.getMessage());
        }
    }

    @Tool(description = "Get details for a specific ticket listing by its ID. "
            + "Returns listing details including section, row, seat, price, status, and seller info.")
    public String getListingDetail(
            @ToolParam(description = "ID of the listing to retrieve", required = true) Long listingId) {
        try {
            if (listingId == null) {
                return errorJson("Listing ID is required");
            }
            ListingDto listing = listingService.getListingById(listingId);
            return objectMapper.writeValueAsString(listing);
        } catch (Exception e) {
            log.error("Error getting listing detail for ID {}: {}", listingId, e.getMessage(), e);
            return errorJson("Failed to get listing detail: " + e.getMessage());
        }
    }

    @Tool(description = "Search for ticket listings across events with filters. "
            + "Combines event search with listing filtering in one call. "
            + "Returns matching listings sorted by price ascending.")
    public String findTickets(
            @ToolParam(description = "Search query text to match event name or artist",
                    required = false) String query,
            @ToolParam(description = "Category slug to filter by: 'concerts', 'sports', 'theater', 'comedy', 'festivals'",
                    required = false) String category,
            @ToolParam(description = "City name to filter events by location",
                    required = false) String city,
            @ToolParam(description = "Only include events on or after this ISO-8601 timestamp",
                    required = false) Instant dateFrom,
            @ToolParam(description = "Only include events on or before this ISO-8601 timestamp",
                    required = false) Instant dateTo,
            @ToolParam(description = "Minimum ticket price filter",
                    required = false) BigDecimal minPrice,
            @ToolParam(description = "Maximum ticket price filter",
                    required = false) BigDecimal maxPrice,
            @ToolParam(description = "Section name filter (e.g. 'Orchestra', 'Balcony')",
                    required = false) String section,
            @ToolParam(description = "Maximum number of results to return (default 10, max 50)",
                    required = false) Integer maxResults) {
        try {
            int limit = (maxResults == null || maxResults <= 0) ? 10 : Math.min(maxResults, 50);

            List<TicketSearchResultDto> results = listingService.searchTickets(
                    query, category, city, minPrice, maxPrice, section, limit);

            return objectMapper.writeValueAsString(results);
        } catch (Exception e) {
            log.error("Error finding tickets: {}", e.getMessage(), e);
            return errorJson("Failed to find tickets: " + e.getMessage());
        }
    }

    private String errorJson(String message) {
        return "{\"error\": \"" + message.replace("\"", "'") + "\"}";
    }
}
