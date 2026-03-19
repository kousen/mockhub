package com.mockhub.mcp.tools;

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
            @ToolParam(description = "Category slug to filter by (e.g. 'rock', 'pop', 'jazz')",
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

    private String errorJson(String message) {
        return "{\"error\": \"" + message.replace("\"", "'") + "\"}";
    }
}
