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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/venues")
@Tag(name = "Venues", description = "Browse and view venue details")
public class VenueController {

    private final VenueService venueService;

    public VenueController(VenueService venueService) {
        this.venueService = venueService;
    }

    @GetMapping
    @Operation(summary = "List venues", description = "Return all venues with optional city filter and pagination")
    @ApiResponse(responseCode = "200", description = "Venues returned successfully")
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
    @Operation(summary = "Get venue by slug", description = "Return full details for a single venue including sections")
    @ApiResponse(responseCode = "200", description = "Venue returned")
    @ApiResponse(responseCode = "404", description = "Venue not found")
    public ResponseEntity<VenueDto> getVenue(
            @Parameter(description = "Venue URL slug", example = "madison-square-garden")
            @PathVariable String slug) {
        return ResponseEntity.ok(venueService.getBySlug(slug));
    }
}
