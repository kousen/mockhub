package com.mockhub.pricing.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mockhub.pricing.dto.PriceHistoryDto;
import com.mockhub.pricing.service.PriceHistoryService;

@RestController
@RequestMapping("/api/v1")
public class PriceController {

    private final PriceHistoryService priceHistoryService;

    public PriceController(PriceHistoryService priceHistoryService) {
        this.priceHistoryService = priceHistoryService;
    }

    @GetMapping("/price-history/event/{eventId}")
    public ResponseEntity<List<PriceHistoryDto>> getPriceHistoryByEventId(
            @PathVariable Long eventId) {
        return ResponseEntity.ok(priceHistoryService.getByEventId(eventId));
    }
}
