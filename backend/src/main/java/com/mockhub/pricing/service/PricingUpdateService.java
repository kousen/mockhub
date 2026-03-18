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

        BigDecimal newMinPrice = event.getBasePrice().multiply(multiplier)
                .setScale(2, RoundingMode.HALF_UP);
        event.setMinPrice(newMinPrice);
        eventRepository.save(event);

        listingService.updateListingPrices(eventId, multiplier);

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
