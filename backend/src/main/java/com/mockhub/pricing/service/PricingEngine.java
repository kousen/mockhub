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

import com.mockhub.cart.repository.CartItemRepository;
import com.mockhub.event.entity.Event;
import com.mockhub.event.repository.EventRepository;

@Service
public class PricingEngine {

    private static final Logger log = LoggerFactory.getLogger(PricingEngine.class);

    private static final BigDecimal MIN_MULTIPLIER = new BigDecimal("0.5");
    private static final BigDecimal MAX_MULTIPLIER = new BigDecimal("3.0");

    private final EventRepository eventRepository;
    private final PricingUpdateService pricingUpdateService;
    private final CartItemRepository cartItemRepository;

    public PricingEngine(EventRepository eventRepository,
                         PricingUpdateService pricingUpdateService,
                         CartItemRepository cartItemRepository) {
        this.eventRepository = eventRepository;
        this.pricingUpdateService = pricingUpdateService;
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

    @Scheduled(fixedRateString = "${mockhub.pricing.update-interval:300000}")
    public void updateAllPricing() {
        List<Event> activeEvents = eventRepository.findAll().stream()
                .filter(event -> "ACTIVE".equals(event.getStatus()))
                .toList();

        for (Event event : activeEvents) {
            BigDecimal multiplier = computeMultiplier(event);
            pricingUpdateService.updateEventPricing(event.getId(), multiplier);
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
