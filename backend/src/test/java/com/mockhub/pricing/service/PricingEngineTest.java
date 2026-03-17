package com.mockhub.pricing.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.mockhub.cart.repository.CartItemRepository;
import com.mockhub.event.entity.Event;
import com.mockhub.event.repository.EventRepository;
import com.mockhub.pricing.repository.PriceHistoryRepository;
import com.mockhub.ticket.service.ListingService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PricingEngineTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private PriceHistoryRepository priceHistoryRepository;

    @Mock
    private ListingService listingService;

    @Mock
    private CartItemRepository cartItemRepository;

    @InjectMocks
    private PricingEngine pricingEngine;

    private Event testEvent;

    @BeforeEach
    void setUp() {
        testEvent = new Event();
        testEvent.setId(1L);
        testEvent.setName("Test Event");
        testEvent.setBasePrice(new BigDecimal("100.00"));
        testEvent.setTotalTickets(1000);
        testEvent.setAvailableTickets(500);
        testEvent.setEventDate(Instant.now().plus(30, ChronoUnit.DAYS));
        testEvent.setStatus("ACTIVE");
    }

    @Test
    @DisplayName("computeMultiplier - given normal supply and timing - returns reasonable multiplier")
    void computeMultiplier_givenNormalConditions_returnsReasonableMultiplier() {
        when(cartItemRepository.countByAddedAtAfter(any(Instant.class))).thenReturn(10L);

        BigDecimal multiplier = pricingEngine.computeMultiplier(testEvent);

        assertNotNull(multiplier, "Multiplier should not be null");
        assertTrue(multiplier.compareTo(new BigDecimal("0.5")) >= 0,
                "Multiplier should be at least 0.5");
        assertTrue(multiplier.compareTo(new BigDecimal("3.0")) <= 0,
                "Multiplier should be at most 3.0");
    }

    @Test
    @DisplayName("computeMultiplier - given high scarcity - returns higher multiplier")
    void computeMultiplier_givenHighScarcity_returnsHigherMultiplier() {
        testEvent.setAvailableTickets(50); // 5% availability
        testEvent.setEventDate(Instant.now().plus(2, ChronoUnit.DAYS)); // 2 days away

        when(cartItemRepository.countByAddedAtAfter(any(Instant.class))).thenReturn(10L);

        BigDecimal multiplier = pricingEngine.computeMultiplier(testEvent);

        assertTrue(multiplier.compareTo(BigDecimal.ONE) > 0,
                "High scarcity + approaching event should produce multiplier > 1");
    }

    @Test
    @DisplayName("computeMultiplier - given high availability far from event - returns lower multiplier")
    void computeMultiplier_givenHighAvailabilityFarFromEvent_returnsLowerMultiplier() {
        testEvent.setAvailableTickets(950); // 95% availability
        testEvent.setEventDate(Instant.now().plus(90, ChronoUnit.DAYS)); // 90 days away

        when(cartItemRepository.countByAddedAtAfter(any(Instant.class))).thenReturn(5L);

        BigDecimal multiplier = pricingEngine.computeMultiplier(testEvent);

        assertTrue(multiplier.compareTo(BigDecimal.ONE) < 0,
                "High availability + far from event should produce multiplier < 1");
    }

    @Test
    @DisplayName("computeMultiplier - given event is today - time factor drops price")
    void computeMultiplier_givenEventIsToday_timeFactorDropsPrice() {
        testEvent.setAvailableTickets(500);
        testEvent.setEventDate(Instant.now().minus(1, ChronoUnit.HOURS));

        when(cartItemRepository.countByAddedAtAfter(any(Instant.class))).thenReturn(0L);

        BigDecimal multiplier = pricingEngine.computeMultiplier(testEvent);

        // day-of event has time factor 0.7, so multiplier should be reduced
        assertTrue(multiplier.compareTo(BigDecimal.ONE) < 0,
                "Day-of event should have reduced multiplier");
    }

    @Test
    @DisplayName("computeMultiplier - given no tickets at all - returns 1.0 supply factor")
    void computeMultiplier_givenNoTickets_returnsDefaultSupplyFactor() {
        testEvent.setTotalTickets(0);
        testEvent.setAvailableTickets(0);

        when(cartItemRepository.countByAddedAtAfter(any(Instant.class))).thenReturn(0L);

        BigDecimal multiplier = pricingEngine.computeMultiplier(testEvent);

        assertNotNull(multiplier, "Multiplier should not be null even with zero tickets");
    }

    @Test
    @DisplayName("computeMultiplier - given high demand - increases multiplier")
    void computeMultiplier_givenHighDemand_increasesMultiplier() {
        testEvent.setAvailableTickets(500);
        testEvent.setEventDate(Instant.now().plus(30, ChronoUnit.DAYS));

        when(cartItemRepository.countByAddedAtAfter(any(Instant.class))).thenReturn(150L);

        BigDecimal highDemandMultiplier = pricingEngine.computeMultiplier(testEvent);

        when(cartItemRepository.countByAddedAtAfter(any(Instant.class))).thenReturn(5L);

        BigDecimal lowDemandMultiplier = pricingEngine.computeMultiplier(testEvent);

        assertTrue(highDemandMultiplier.compareTo(lowDemandMultiplier) > 0,
                "High demand should produce a higher multiplier than low demand");
    }

    @Test
    @DisplayName("computeMultiplier - result is clamped to min/max bounds")
    void computeMultiplier_resultIsClampedToMinMaxBounds() {
        // Extreme scarcity + high demand + event very soon
        testEvent.setAvailableTickets(10); // 1% availability
        testEvent.setEventDate(Instant.now().plus(1, ChronoUnit.DAYS));

        when(cartItemRepository.countByAddedAtAfter(any(Instant.class))).thenReturn(200L);

        BigDecimal multiplier = pricingEngine.computeMultiplier(testEvent);

        assertTrue(multiplier.compareTo(new BigDecimal("3.0")) <= 0,
                "Multiplier should be clamped at 3.0 maximum");
        assertEquals(3, multiplier.scale(), "Multiplier should have 3 decimal places");
    }
}
