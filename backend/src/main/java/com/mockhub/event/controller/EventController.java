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
import com.mockhub.venue.dto.SectionDto;
import com.mockhub.venue.dto.SectionAvailabilityDto;
import com.mockhub.ticket.service.TicketService;

@RestController
@RequestMapping("/api/v1/events")
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
    public ResponseEntity<PagedResponse<EventSummaryDto>> listEvents(
            @ModelAttribute EventSearchRequest request) {
        return ResponseEntity.ok(eventService.listEvents(request));
    }

    @GetMapping("/featured")
    public ResponseEntity<List<EventSummaryDto>> listFeatured() {
        return ResponseEntity.ok(eventService.listFeatured());
    }

    @GetMapping("/{slug}")
    public ResponseEntity<EventDto> getEvent(@PathVariable String slug) {
        return ResponseEntity.ok(eventService.getBySlug(slug));
    }

    @GetMapping("/{slug}/listings")
    public ResponseEntity<List<ListingDto>> getEventListings(@PathVariable String slug) {
        return ResponseEntity.ok(listingService.getActiveListingsByEventSlug(slug));
    }

    @GetMapping("/{slug}/price-history")
    public ResponseEntity<List<PriceHistoryDto>> getPriceHistory(@PathVariable String slug) {
        return ResponseEntity.ok(priceHistoryService.getByEventSlug(slug));
    }

    @GetMapping("/{slug}/sections")
    public ResponseEntity<List<SectionAvailabilityDto>> getEventSections(@PathVariable String slug) {
        return ResponseEntity.ok(ticketService.getSectionAvailability(slug));
    }

    @PostMapping
    public ResponseEntity<EventDto> createEvent(@Valid @RequestBody EventCreateRequest request) {
        EventDto created = eventService.createEvent(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<EventDto> updateEvent(@PathVariable Long id,
                                                 @Valid @RequestBody EventCreateRequest request) {
        return ResponseEntity.ok(eventService.updateEvent(id, request));
    }
}
