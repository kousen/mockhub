package com.mockhub.pricing.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PricingEngineTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private PricingUpdateService pricingUpdateService;

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
    @DisplayName("computeMultiplier - given normal supply and timing - returns 1.000 multiplier")
    void computeMultiplier_givenNormalConditions_returnsReasonableMultiplier() {
        when(cartItemRepository.countByAddedAtAfter(any(Instant.class))).thenReturn(10L);

        BigDecimal multiplier = pricingEngine.computeMultiplier(testEvent);

        assertEquals(new BigDecimal("1.000"), multiplier);
    }

    @Test
    @DisplayName("computeMultiplier - given high scarcity - returns higher multiplier")
    void computeMultiplier_givenHighScarcity_returnsHigherMultiplier() {
        testEvent.setAvailableTickets(50); // 5% availability -> 1.8
        testEvent.setEventDate(Instant.now().plus(2, ChronoUnit.DAYS)); // < 3 days -> 1.5

        when(cartItemRepository.countByAddedAtAfter(any(Instant.class))).thenReturn(10L);

        BigDecimal multiplier = pricingEngine.computeMultiplier(testEvent);

        // 1.8 * 1.5 * 1.0 = 2.700
        assertEquals(new BigDecimal("2.700"), multiplier);
    }

    @Test
    @DisplayName("computeMultiplier - given high availability far from event - returns lower multiplier")
    void computeMultiplier_givenHighAvailabilityFarFromEvent_returnsLowerMultiplier() {
        testEvent.setAvailableTickets(950); // 95% availability -> 0.85
        testEvent.setEventDate(Instant.now().plus(90, ChronoUnit.DAYS)); // > 60 days -> 0.9

        when(cartItemRepository.countByAddedAtAfter(any(Instant.class))).thenReturn(5L);

        BigDecimal multiplier = pricingEngine.computeMultiplier(testEvent);

        // 0.85 * 0.9 * 1.0 = 0.765
        assertEquals(new BigDecimal("0.765"), multiplier);
    }

    @Test
    @DisplayName("computeMultiplier - given event is today - time factor drops price")
    void computeMultiplier_givenEventIsToday_timeFactorDropsPrice() {
        testEvent.setAvailableTickets(500); // 50% -> 1.0
        testEvent.setEventDate(Instant.now().minus(1, ChronoUnit.HOURS)); // <= 0 days -> 0.7

        when(cartItemRepository.countByAddedAtAfter(any(Instant.class))).thenReturn(0L);

        BigDecimal multiplier = pricingEngine.computeMultiplier(testEvent);

        assertEquals(new BigDecimal("0.700"), multiplier);
    }

    @Test
    @DisplayName("computeMultiplier - given no tickets at all - returns 1.0 supply factor")
    void computeMultiplier_givenNoTickets_returnsDefaultSupplyFactor() {
        testEvent.setTotalTickets(0);
        testEvent.setAvailableTickets(0);

        when(cartItemRepository.countByAddedAtAfter(any(Instant.class))).thenReturn(0L);

        BigDecimal multiplier = pricingEngine.computeMultiplier(testEvent);

        assertEquals(new BigDecimal("1.000"), multiplier);
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

        assertEquals(new BigDecimal("1.300"), highDemandMultiplier);
        assertEquals(new BigDecimal("1.000"), lowDemandMultiplier);
        assertTrue(highDemandMultiplier.compareTo(lowDemandMultiplier) > 0,
                "High demand should produce a higher multiplier than low demand");
    }

    @Test
    @DisplayName("computeMultiplier - result is clamped to min/max bounds")
    void computeMultiplier_resultIsClampedToMinMaxBounds() {
        // Extreme scarcity (1.8) + event within 3 days (1.5) + high demand (1.3) = 3.51 -> clamped to 3.000
        testEvent.setAvailableTickets(10); // 1% availability -> 1.8
        testEvent.setEventDate(Instant.now().plus(2, ChronoUnit.DAYS)); // 1 day after truncation -> 1.5

        when(cartItemRepository.countByAddedAtAfter(any(Instant.class))).thenReturn(200L);

        BigDecimal multiplier = pricingEngine.computeMultiplier(testEvent);

        assertEquals(new BigDecimal("3.000"), multiplier,
                "Multiplier should be clamped at 3.000 maximum");
    }

    @Test
    @DisplayName("computeMultiplier - given supply ratio boundaries - returns expected tier factors")
    void computeMultiplier_givenSupplyRatioBoundaries_returnsExpectedTierFactors() {
        testEvent.setEventDate(Instant.now().plus(30, ChronoUnit.DAYS)); // timeFactor = 1.0
        when(cartItemRepository.countByAddedAtAfter(any(Instant.class))).thenReturn(0L); // demandFactor = 1.0

        // Exactly 90% (900 / 1000 = 0.90) -> 0.85
        testEvent.setAvailableTickets(900);
        assertEquals(new BigDecimal("0.850"), pricingEngine.computeMultiplier(testEvent));

        // Just below 90% (899 / 1000 = 0.899) -> 1.0
        testEvent.setAvailableTickets(899);
        assertEquals(new BigDecimal("1.000"), pricingEngine.computeMultiplier(testEvent));

        // Exactly 50% (500 / 1000 = 0.50) -> 1.0
        testEvent.setAvailableTickets(500);
        assertEquals(new BigDecimal("1.000"), pricingEngine.computeMultiplier(testEvent));

        // Just below 50% (499 / 1000 = 0.499) -> 1.3
        testEvent.setAvailableTickets(499);
        assertEquals(new BigDecimal("1.300"), pricingEngine.computeMultiplier(testEvent));

        // Exactly 20% (200 / 1000 = 0.20) -> 1.3
        testEvent.setAvailableTickets(200);
        assertEquals(new BigDecimal("1.300"), pricingEngine.computeMultiplier(testEvent));

        // Just below 20% (199 / 1000 = 0.199) -> 1.8
        testEvent.setAvailableTickets(199);
        assertEquals(new BigDecimal("1.800"), pricingEngine.computeMultiplier(testEvent));
    }

    @Test
    @DisplayName("computeMultiplier - given time factor boundaries - returns expected tier factors")
    void computeMultiplier_givenTimeFactorBoundaries_returnsExpectedTierFactors() {
        testEvent.setAvailableTickets(500); // supplyFactor = 1.0
        when(cartItemRepository.countByAddedAtAfter(any(Instant.class))).thenReturn(0L); // demandFactor = 1.0

        // 0 days (12 hours from now truncates to 0 days) -> 0.7
        testEvent.setEventDate(Instant.now().plus(12, ChronoUnit.HOURS));
        assertEquals(new BigDecimal("0.700"), pricingEngine.computeMultiplier(testEvent));

        // 2 days (< 3 days) -> 1.5
        testEvent.setEventDate(Instant.now().plus(2, ChronoUnit.DAYS).plus(12, ChronoUnit.HOURS));
        assertEquals(new BigDecimal("1.500"), pricingEngine.computeMultiplier(testEvent));

        // 3 days (3 <= days < 14) -> 1.2
        testEvent.setEventDate(Instant.now().plus(3, ChronoUnit.DAYS).plus(12, ChronoUnit.HOURS));
        assertEquals(new BigDecimal("1.200"), pricingEngine.computeMultiplier(testEvent));

        // 14 days (14 <= days <= 60) -> 1.0
        testEvent.setEventDate(Instant.now().plus(14, ChronoUnit.DAYS).plus(12, ChronoUnit.HOURS));
        assertEquals(new BigDecimal("1.000"), pricingEngine.computeMultiplier(testEvent));

        // 60 days (<= 60 days) -> 1.0
        testEvent.setEventDate(Instant.now().plus(60, ChronoUnit.DAYS).plus(12, ChronoUnit.HOURS));
        assertEquals(new BigDecimal("1.000"), pricingEngine.computeMultiplier(testEvent));

        // 61 days (> 60 days) -> 0.9
        testEvent.setEventDate(Instant.now().plus(61, ChronoUnit.DAYS).plus(12, ChronoUnit.HOURS));
        assertEquals(new BigDecimal("0.900"), pricingEngine.computeMultiplier(testEvent));
    }

    @Test
    @DisplayName("computeMultiplier - given demand factor boundaries - returns expected tier factors")
    void computeMultiplier_givenDemandFactorBoundaries_returnsExpectedTierFactors() {
        testEvent.setAvailableTickets(500); // supplyFactor = 1.0
        testEvent.setEventDate(Instant.now().plus(30, ChronoUnit.DAYS)); // timeFactor = 1.0

        // 19 recent items (< 20) -> 1.0
        when(cartItemRepository.countByAddedAtAfter(any(Instant.class))).thenReturn(19L);
        assertEquals(new BigDecimal("1.000"), pricingEngine.computeMultiplier(testEvent));

        // 20 recent items (>= 20) -> 1.05
        when(cartItemRepository.countByAddedAtAfter(any(Instant.class))).thenReturn(20L);
        assertEquals(new BigDecimal("1.050"), pricingEngine.computeMultiplier(testEvent));

        // 49 recent items (20..49) -> 1.05
        when(cartItemRepository.countByAddedAtAfter(any(Instant.class))).thenReturn(49L);
        assertEquals(new BigDecimal("1.050"), pricingEngine.computeMultiplier(testEvent));

        // 50 recent items (>= 50) -> 1.15
        when(cartItemRepository.countByAddedAtAfter(any(Instant.class))).thenReturn(50L);
        assertEquals(new BigDecimal("1.150"), pricingEngine.computeMultiplier(testEvent));

        // 99 recent items (50..99) -> 1.15
        when(cartItemRepository.countByAddedAtAfter(any(Instant.class))).thenReturn(99L);
        assertEquals(new BigDecimal("1.150"), pricingEngine.computeMultiplier(testEvent));

        // 100 recent items (>= 100) -> 1.3
        when(cartItemRepository.countByAddedAtAfter(any(Instant.class))).thenReturn(100L);
        assertEquals(new BigDecimal("1.300"), pricingEngine.computeMultiplier(testEvent));
    }

    @Test
    @DisplayName("updateAllPricing - given active and inactive events - updates only active events")
    void updateAllPricing_givenActiveAndInactiveEvents_updatesOnlyActiveEvents() {
        Event inactiveEvent = new Event();
        inactiveEvent.setId(2L);
        inactiveEvent.setStatus("COMPLETED");

        when(eventRepository.findAll()).thenReturn(List.of(testEvent, inactiveEvent));
        when(cartItemRepository.countByAddedAtAfter(any(Instant.class))).thenReturn(0L);

        pricingEngine.updateAllPricing();

        verify(pricingUpdateService).updateEventPricing(1L, new BigDecimal("1.000"));
        verify(pricingUpdateService, never()).updateEventPricing(
                org.mockito.ArgumentMatchers.eq(2L), any(BigDecimal.class));
    }
}
