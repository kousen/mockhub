package com.mockhub.pricing.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mockhub.cart.repository.CartItemRepository;
import com.mockhub.event.entity.Event;
import com.mockhub.event.repository.EventRepository;
import com.mockhub.pricing.entity.PriceHistory;
import com.mockhub.pricing.repository.PriceHistoryRepository;
import com.mockhub.ticket.service.ListingService;

@Service
public class PricingEngine {

    private static final Logger log = LoggerFactory.getLogger(PricingEngine.class);

    private static final BigDecimal MIN_MULTIPLIER = new BigDecimal("0.5");
    private static final BigDecimal MAX_MULTIPLIER = new BigDecimal("3.0");

    private final EventRepository eventRepository;
    private final PriceHistoryRepository priceHistoryRepository;
    private final ListingService listingService;
    private final CartItemRepository cartItemRepository;

    public PricingEngine(EventRepository eventRepository,
                         PriceHistoryRepository priceHistoryRepository,
                         ListingService listingService,
                         CartItemRepository cartItemRepository) {
        this.eventRepository = eventRepository;
        this.priceHistoryRepository = priceHistoryRepository;
        this.listingService = listingService;
        this.cartItemRepository = cartItemRepository;
    }

    public BigDecimal computeMultiplier(Event event) {
        BigDecimal supplyFactor = computeSupplyFactor(event);
        BigDecimal timeFactor = computeTimeFactor(event);
        BigDecimal demandFactor = computeDemandFactor();

        BigDecimal rawMultiplier = supplyFactor.multiply(timeFactor).multiply(demandFactor);
        return clamp(rawMultiplier, MIN_MULTIPLIER, MAX_MULTIPLIER)
                .setScale(3, RoundingMode.HALF_UP);
    }

    @Transactional
    public void updateEventPricing(Long eventId) {
        Event event = eventRepository.findById(eventId).orElse(null);
        if (event == null || !"ACTIVE".equals(event.getStatus())) {
            return;
        }

        BigDecimal multiplier = computeMultiplier(event);

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

    @Scheduled(fixedRateString = "${mockhub.pricing.update-interval:300000}")
    @Transactional
    public void updateAllPricing() {
        List<Event> activeEvents = eventRepository.findAll().stream()
                .filter(event -> "ACTIVE".equals(event.getStatus()))
                .toList();

        for (Event event : activeEvents) {
            updateEventPricing(event.getId());
        }

        log.info("Updated pricing for {} active events", activeEvents.size());
    }

    /**
     * Supply factor based on available_tickets / total_tickets ratio.
     * Higher availability means lower prices; scarcity drives prices up.
     */
    private BigDecimal computeSupplyFactor(Event event) {
        if (event.getTotalTickets() == 0) {
            return BigDecimal.ONE;
        }

        double ratio = (double) event.getAvailableTickets() / event.getTotalTickets();

        if (ratio >= 0.9) {
            return new BigDecimal("0.85");
        } else if (ratio >= 0.5) {
            return BigDecimal.ONE;
        } else if (ratio >= 0.2) {
            return new BigDecimal("1.3");
        } else {
            return new BigDecimal("1.8");
        }
    }

    /**
     * Time factor based on days until the event.
     * Prices increase as the event approaches, then drop on the day of the event.
     */
    private BigDecimal computeTimeFactor(Event event) {
        long daysToEvent = computeDaysToEvent(event);

        if (daysToEvent <= 0) {
            return new BigDecimal("0.7");
        } else if (daysToEvent < 3) {
            return new BigDecimal("1.5");
        } else if (daysToEvent < 14) {
            return new BigDecimal("1.2");
        } else if (daysToEvent <= 60) {
            return BigDecimal.ONE;
        } else {
            return new BigDecimal("0.9");
        }
    }

    private BigDecimal computeSupplyRatio(Event event) {
        if (event.getTotalTickets() == 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(event.getAvailableTickets())
                .divide(BigDecimal.valueOf(event.getTotalTickets()), 4, RoundingMode.HALF_UP);
    }

    private long computeDaysToEvent(Event event) {
        Duration duration = Duration.between(Instant.now(), event.getEventDate());
        return duration.toDays();
    }

    /**
     * Demand factor based on recent cart activity (items added in the last 24 hours).
     * Higher cart activity signals stronger demand and drives prices up slightly.
     */
    private BigDecimal computeDemandFactor() {
        Instant cutoff = Instant.now().minus(Duration.ofHours(24));
        long recentCartItems = cartItemRepository.countByAddedAtAfter(cutoff);

        if (recentCartItems >= 100) {
            return new BigDecimal("1.3");
        } else if (recentCartItems >= 50) {
            return new BigDecimal("1.15");
        } else if (recentCartItems >= 20) {
            return new BigDecimal("1.05");
        } else {
            return BigDecimal.ONE;
        }
    }

    private BigDecimal clamp(BigDecimal value, BigDecimal min, BigDecimal max) {
        if (value.compareTo(min) < 0) {
            return min;
        }
        if (value.compareTo(max) > 0) {
            return max;
        }
        return value;
    }
}
