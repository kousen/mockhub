package com.mockhub.pricing.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mockhub.event.entity.Event;
import com.mockhub.event.repository.EventRepository;
import com.mockhub.pricing.entity.PriceHistory;
import com.mockhub.pricing.repository.PriceHistoryRepository;
import com.mockhub.ticket.service.ListingService;

/**
 * Handles individual event price updates in their own transaction.
 * Separated from PricingEngine so that Spring's proxy can manage
 * the transaction boundary for each event independently.
 */
@Service
public class PricingUpdateService {

    private static final Logger log = LoggerFactory.getLogger(PricingUpdateService.class);

    private final EventRepository eventRepository;
    private final PriceHistoryRepository priceHistoryRepository;
    private final ListingService listingService;

    public PricingUpdateService(EventRepository eventRepository,
                                PriceHistoryRepository priceHistoryRepository,
                                ListingService listingService) {
        this.eventRepository = eventRepository;
        this.priceHistoryRepository = priceHistoryRepository;
        this.listingService = listingService;
    }

    @Transactional
    public void updateEventPricing(Long eventId, BigDecimal multiplier) {
        Event event = eventRepository.findById(eventId).orElse(null);
        if (event == null || !"ACTIVE".equals(event.getStatus())) {
            return;
        }

        // Skip-if-unchanged: with static supply/demand the engine used to write
        // an identical snapshot every 5 minutes (3.3M rows in production, one
        // real price change). Same multiplier means computed listing prices are
        // already correct, so only a listing-set change can move the price.
        PriceHistory lastSnapshot = priceHistoryRepository
                .findFirstByEventIdOrderByRecordedAtDesc(eventId).orElse(null);
        boolean multiplierUnchanged = lastSnapshot != null
                && lastSnapshot.getMultiplier().compareTo(multiplier) == 0;

        if (!multiplierUnchanged) {
            listingService.updateListingPrices(eventId, multiplier);
        }

        BigDecimal[] priceRange = listingService.getComputedPriceRange(eventId);
        BigDecimal newMinPrice = priceRange[0] != null
                ? priceRange[0]
                : event.getBasePrice().multiply(multiplier).setScale(2, RoundingMode.HALF_UP);
        BigDecimal newMaxPrice = priceRange[1] != null
                ? priceRange[1]
                : newMinPrice;
        if (multiplierUnchanged && lastSnapshot.getPrice().compareTo(newMinPrice) == 0) {
            return; // nothing moved — no event write, no snapshot
        }

        event.setMinPrice(newMinPrice);
        event.setMaxPrice(newMaxPrice);
        eventRepository.save(event);

        BigDecimal supplyRatio = computeSupplyRatio(event);
        long daysToEvent = computeDaysToEvent(event);

        PriceHistory history = new PriceHistory();
        history.setEvent(event);
        history.setPrice(newMinPrice);
        history.setMultiplier(multiplier);
        history.setSupplyRatio(supplyRatio.setScale(4, RoundingMode.HALF_UP));
        history.setDaysToEvent((int) daysToEvent);
        history.setRecordedAt(Instant.now());

        priceHistoryRepository.save(history);
        log.debug("Updated pricing for event {}: multiplier={}", eventId, multiplier);
    }

    private BigDecimal computeSupplyRatio(Event event) {
        if (event.getTotalTickets() == 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(event.getAvailableTickets())
                .divide(BigDecimal.valueOf(event.getTotalTickets()), 4, RoundingMode.HALF_UP);
    }

    private long computeDaysToEvent(Event event) {
        return java.time.Duration.between(Instant.now(), event.getEventDate()).toDays();
    }
}
