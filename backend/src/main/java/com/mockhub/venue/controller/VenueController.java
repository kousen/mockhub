package com.mockhub.venue.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.mockhub.common.dto.PagedResponse;
import com.mockhub.venue.dto.VenueDto;
import com.mockhub.venue.dto.VenueSummaryDto;
import com.mockhub.venue.service.VenueService;

@RestController
@RequestMapping("/api/v1/venues")
public class VenueController {

    private final VenueService venueService;

    public VenueController(VenueService venueService) {
        this.venueService = venueService;
    }

    @GetMapping
    public ResponseEntity<PagedResponse<VenueSummaryDto>> listVenues(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String city) {
        if (city != null && !city.isBlank()) {
            return ResponseEntity.ok(venueService.getByCity(city, page, size));
        }
        return ResponseEntity.ok(venueService.listAll(page, size));
    }

    @GetMapping("/{slug}")
    public ResponseEntity<VenueDto> getVenue(@PathVariable String slug) {
        return ResponseEntity.ok(venueService.getBySlug(slug));
    }
}
