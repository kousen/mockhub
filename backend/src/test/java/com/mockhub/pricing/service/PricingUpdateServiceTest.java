package com.mockhub.pricing.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.mockhub.event.entity.Event;
import com.mockhub.event.repository.EventRepository;
import com.mockhub.pricing.entity.PriceHistory;
import com.mockhub.pricing.repository.PriceHistoryRepository;
import com.mockhub.ticket.service.ListingService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PricingUpdateServiceTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private PriceHistoryRepository priceHistoryRepository;

    @Mock
    private ListingService listingService;

    @InjectMocks
    private PricingUpdateService pricingUpdateService;

    private Event testEvent;

    @BeforeEach
    void setUp() {
        testEvent = new Event();
        testEvent.setId(1L);
        testEvent.setName("Test Concert");
        testEvent.setSlug("test-concert");
        testEvent.setStatus("ACTIVE");
        testEvent.setBasePrice(new BigDecimal("100.00"));
        testEvent.setMinPrice(new BigDecimal("100.00"));
        testEvent.setTotalTickets(1000);
        testEvent.setAvailableTickets(750);
        testEvent.setEventDate(Instant.now().plus(30, ChronoUnit.DAYS));
    }

    @Test
    @DisplayName("updateEventPricing - given active event - updates min price and saves history")
    void updateEventPricing_givenActiveEvent_updatesPriceAndSavesHistory() {
        BigDecimal multiplier = new BigDecimal("1.250");
        when(eventRepository.findById(1L)).thenReturn(Optional.of(testEvent));
        when(listingService.getComputedPriceRange(1L)).thenReturn(new BigDecimal[]{null, null});

        pricingUpdateService.updateEventPricing(1L, multiplier);

        BigDecimal expectedPrice = new BigDecimal("125.00");
        assertEquals(expectedPrice, testEvent.getMinPrice(),
                "Event min price should be updated to basePrice * multiplier when no listings");
        verify(eventRepository).save(testEvent);
        verify(listingService).updateListingPrices(1L, multiplier);
        verify(priceHistoryRepository).save(any(PriceHistory.class));
    }

    @Test
    @DisplayName("updateEventPricing - given active event - saves correct PriceHistory fields")
    void updateEventPricing_givenActiveEvent_savesCorrectPriceHistoryFields() {
        BigDecimal multiplier = new BigDecimal("1.500");
        when(eventRepository.findById(1L)).thenReturn(Optional.of(testEvent));
        when(listingService.getComputedPriceRange(1L)).thenReturn(new BigDecimal[]{null, null});

        pricingUpdateService.updateEventPricing(1L, multiplier);

        ArgumentCaptor<PriceHistory> captor = ArgumentCaptor.forClass(PriceHistory.class);
        verify(priceHistoryRepository).save(captor.capture());

        PriceHistory savedHistory = captor.getValue();
        assertEquals(testEvent, savedHistory.getEvent(),
                "PriceHistory event should match the test event");
        assertEquals(new BigDecimal("150.00"), savedHistory.getPrice(),
                "PriceHistory price should be 100.00 * 1.500 = 150.00");
        assertEquals(multiplier, savedHistory.getMultiplier(),
                "PriceHistory multiplier should match the input multiplier");
        assertEquals(new BigDecimal("0.7500"), savedHistory.getSupplyRatio(),
                "Supply ratio should be 750/1000 = 0.7500");
        assertNotNull(savedHistory.getRecordedAt(),
                "Recorded timestamp should be set");
    }

    @Test
    @DisplayName("updateEventPricing - given non-existent event - does nothing")
    void updateEventPricing_givenNonExistentEvent_doesNothing() {
        when(eventRepository.findById(999L)).thenReturn(Optional.empty());

        pricingUpdateService.updateEventPricing(999L, new BigDecimal("1.100"));

        verify(eventRepository, never()).save(any(Event.class));
        verify(listingService, never()).updateListingPrices(any(), any());
        verify(priceHistoryRepository, never()).save(any(PriceHistory.class));
    }

    @Test
    @DisplayName("updateEventPricing - given inactive event - does nothing")
    void updateEventPricing_givenInactiveEvent_doesNothing() {
        testEvent.setStatus("CANCELLED");
        when(eventRepository.findById(1L)).thenReturn(Optional.of(testEvent));

        pricingUpdateService.updateEventPricing(1L, new BigDecimal("1.100"));

        verify(eventRepository, never()).save(any(Event.class));
        verify(listingService, never()).updateListingPrices(any(), any());
        verify(priceHistoryRepository, never()).save(any(PriceHistory.class));
    }

    @Test
    @DisplayName("updateEventPricing - given multiplier - calculates correct price with rounding")
    void updateEventPricing_givenMultiplier_calculatesCorrectPrice() {
        BigDecimal multiplier = new BigDecimal("1.333");
        when(eventRepository.findById(1L)).thenReturn(Optional.of(testEvent));
        when(listingService.getComputedPriceRange(1L)).thenReturn(new BigDecimal[]{null, null});

        pricingUpdateService.updateEventPricing(1L, multiplier);

        BigDecimal expectedPrice = new BigDecimal("100.00").multiply(multiplier)
                .setScale(2, RoundingMode.HALF_UP);
        assertEquals(expectedPrice, testEvent.getMinPrice(),
                "Min price should be basePrice * multiplier rounded to 2 decimal places");
    }

    @Test
    @DisplayName("updateEventPricing - given event with zero total tickets - saves zero supply ratio")
    void updateEventPricing_givenEventWithZeroTotalTickets_savesZeroSupplyRatio() {
        testEvent.setTotalTickets(0);
        testEvent.setAvailableTickets(0);
        when(eventRepository.findById(1L)).thenReturn(Optional.of(testEvent));
        when(listingService.getComputedPriceRange(1L)).thenReturn(new BigDecimal[]{null, null});

        pricingUpdateService.updateEventPricing(1L, new BigDecimal("1.000"));

        ArgumentCaptor<PriceHistory> captor = ArgumentCaptor.forClass(PriceHistory.class);
        verify(priceHistoryRepository).save(captor.capture());

        PriceHistory savedHistory = captor.getValue();
        assertEquals(new BigDecimal("0.0000"), savedHistory.getSupplyRatio(),
                "Supply ratio should be zero when total tickets is zero");
    }

    @Test
    @DisplayName("updateEventPricing - given event with COMPLETED status - does nothing")
    void updateEventPricing_givenCompletedEvent_doesNothing() {
        testEvent.setStatus("COMPLETED");
        when(eventRepository.findById(1L)).thenReturn(Optional.of(testEvent));

        pricingUpdateService.updateEventPricing(1L, new BigDecimal("1.200"));

        verify(eventRepository, never()).save(any(Event.class));
        verify(priceHistoryRepository, never()).save(any(PriceHistory.class));
    }
}
