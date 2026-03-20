package com.mockhub.event.controller;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mockhub.common.dto.PagedResponse;
import com.mockhub.event.dto.EventCreateRequest;
import com.mockhub.event.dto.EventDto;
import com.mockhub.event.dto.EventSearchRequest;
import com.mockhub.event.dto.EventSummaryDto;
import com.mockhub.event.service.EventService;
import com.mockhub.pricing.dto.PriceHistoryDto;
import com.mockhub.pricing.service.PriceHistoryService;
import com.mockhub.ticket.dto.ListingDto;
import com.mockhub.ticket.service.ListingService;
import com.mockhub.venue.dto.SectionAvailabilityDto;
import com.mockhub.ticket.service.TicketService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/events")
@Tag(name = "Events", description = "Browse, search, and manage events")
public class EventController {

    private final EventService eventService;
    private final ListingService listingService;
    private final PriceHistoryService priceHistoryService;
    private final TicketService ticketService;

    public EventController(EventService eventService,
                           ListingService listingService,
                           PriceHistoryService priceHistoryService,
                           TicketService ticketService) {
        this.eventService = eventService;
        this.listingService = listingService;
        this.priceHistoryService = priceHistoryService;
        this.ticketService = ticketService;
    }

    @GetMapping
    @Operation(summary = "List events", description = "Search and filter events with pagination")
    @ApiResponse(responseCode = "200", description = "Events returned successfully")
    public ResponseEntity<PagedResponse<EventSummaryDto>> listEvents(
            @ModelAttribute EventSearchRequest request) {
        return ResponseEntity.ok(eventService.listEvents(request));
    }

    @GetMapping("/featured")
    @Operation(summary = "List featured events", description = "Return all currently featured events")
    @ApiResponse(responseCode = "200", description = "Featured events returned")
    public ResponseEntity<List<EventSummaryDto>> listFeatured() {
        return ResponseEntity.ok(eventService.listFeatured());
    }

    @GetMapping("/{slug}")
    @Operation(summary = "Get event by slug", description = "Return full details for a single event")
    @ApiResponse(responseCode = "200", description = "Event returned")
    @ApiResponse(responseCode = "404", description = "Event not found")
    public ResponseEntity<EventDto> getEvent(
            @Parameter(description = "Event URL slug", example = "taylor-swift-eras-tour-1")
            @PathVariable String slug) {
        return ResponseEntity.ok(eventService.getBySlug(slug));
    }

    @GetMapping("/{slug}/listings")
    @Operation(summary = "Get event listings", description = "Return all active ticket listings for an event")
    @ApiResponse(responseCode = "200", description = "Listings returned")
    @ApiResponse(responseCode = "404", description = "Event not found")
    public ResponseEntity<List<ListingDto>> getEventListings(@PathVariable String slug) {
        return ResponseEntity.ok(listingService.getActiveListingsByEventSlug(slug));
    }

    @GetMapping("/{slug}/price-history")
    @Operation(summary = "Get price history", description = "Return historical price data for an event")
    @ApiResponse(responseCode = "200", description = "Price history returned")
    public ResponseEntity<List<PriceHistoryDto>> getPriceHistory(@PathVariable String slug) {
        return ResponseEntity.ok(priceHistoryService.getByEventSlug(slug));
    }

    @GetMapping("/{slug}/sections")
    @Operation(summary = "Get section availability", description = "Return seat availability by section for an event")
    @ApiResponse(responseCode = "200", description = "Section availability returned")
    public ResponseEntity<List<SectionAvailabilityDto>> getEventSections(@PathVariable String slug) {
        return ResponseEntity.ok(ticketService.getSectionAvailability(slug));
    }

    @PostMapping
    @Operation(summary = "Create event", description = "Create a new event (admin only)")
    @ApiResponse(responseCode = "201", description = "Event created")
    @ApiResponse(responseCode = "400", description = "Invalid request")
    public ResponseEntity<EventDto> createEvent(@Valid @RequestBody EventCreateRequest request) {
        EventDto created = eventService.createEvent(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update event", description = "Update an existing event (admin only)")
    @ApiResponse(responseCode = "200", description = "Event updated")
    @ApiResponse(responseCode = "404", description = "Event not found")
    public ResponseEntity<EventDto> updateEvent(@PathVariable Long id,
                                                 @Valid @RequestBody EventCreateRequest request) {
        return ResponseEntity.ok(eventService.updateEvent(id, request));
    }
}
