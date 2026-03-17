package com.mockhub.pricing.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mockhub.pricing.dto.PriceHistoryDto;
import com.mockhub.pricing.service.PriceHistoryService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Pricing", description = "Price history and analytics")
public class PriceController {

    private final PriceHistoryService priceHistoryService;

    public PriceController(PriceHistoryService priceHistoryService) {
        this.priceHistoryService = priceHistoryService;
    }

    @GetMapping("/price-history/event/{eventId}")
    @Operation(summary = "Get price history by event ID", description = "Return historical price snapshots for an event")
    @ApiResponse(responseCode = "200", description = "Price history returned")
    public ResponseEntity<List<PriceHistoryDto>> getPriceHistoryByEventId(
            @PathVariable Long eventId) {
        return ResponseEntity.ok(priceHistoryService.getByEventId(eventId));
    }
}
