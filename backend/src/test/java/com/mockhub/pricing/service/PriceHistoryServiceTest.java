package com.mockhub.pricing.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.mockhub.common.exception.ResourceNotFoundException;
import com.mockhub.event.entity.Event;
import com.mockhub.event.repository.EventRepository;
import com.mockhub.pricing.dto.PriceHistoryDto;
import com.mockhub.pricing.entity.PriceHistory;
import com.mockhub.pricing.repository.PriceHistoryRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PriceHistoryServiceTest {

    @Mock
    private PriceHistoryRepository priceHistoryRepository;

    @Mock
    private EventRepository eventRepository;

    @InjectMocks
    private PriceHistoryService priceHistoryService;

    private Event testEvent;
    private PriceHistory historyEntry1;
    private PriceHistory historyEntry2;

    @BeforeEach
    void setUp() {
        testEvent = new Event();
        testEvent.setId(1L);
        testEvent.setName("Test Event");
        testEvent.setSlug("test-event");

        historyEntry1 = new PriceHistory();
        historyEntry1.setId(10L);
        historyEntry1.setEvent(testEvent);
        historyEntry1.setPrice(new BigDecimal("120.00"));
        historyEntry1.setMultiplier(new BigDecimal("1.200"));
        historyEntry1.setSupplyRatio(new BigDecimal("0.7500"));
        historyEntry1.setDemandScore(new BigDecimal("0.8500"));
        historyEntry1.setDaysToEvent(14);
        historyEntry1.setRecordedAt(Instant.parse("2026-03-10T12:00:00Z"));

        historyEntry2 = new PriceHistory();
        historyEntry2.setId(11L);
        historyEntry2.setEvent(testEvent);
        historyEntry2.setPrice(new BigDecimal("135.00"));
        historyEntry2.setMultiplier(new BigDecimal("1.350"));
        historyEntry2.setSupplyRatio(new BigDecimal("0.5000"));
        historyEntry2.setDemandScore(new BigDecimal("0.9200"));
        historyEntry2.setDaysToEvent(7);
        historyEntry2.setRecordedAt(Instant.parse("2026-03-17T12:00:00Z"));
    }

    @Test
    @DisplayName("getByEventSlug - given existing slug - returns history mapped to DTOs")
    void getByEventSlug_givenExistingSlug_returnsHistory() {
        when(eventRepository.findBySlug("test-event")).thenReturn(Optional.of(testEvent));
        when(priceHistoryRepository.findByEventIdOrderByRecordedAtDesc(1L))
                .thenReturn(List.of(historyEntry2, historyEntry1));

        List<PriceHistoryDto> result = priceHistoryService.getByEventSlug("test-event");

        assertNotNull(result, "Result should not be null");
        assertEquals(2, result.size(), "Should return two history entries");

        PriceHistoryDto firstDto = result.get(0);
        assertEquals(11L, firstDto.id(), "First entry ID should match");
        assertEquals(1L, firstDto.eventId(), "Event ID should match");
        assertEquals(new BigDecimal("135.00"), firstDto.price(), "Price should match");
        assertEquals(new BigDecimal("1.350"), firstDto.multiplier(), "Multiplier should match");
        assertEquals(new BigDecimal("0.5000"), firstDto.supplyRatio(), "Supply ratio should match");
        assertEquals(new BigDecimal("0.9200"), firstDto.demandScore(), "Demand score should match");
        assertEquals(7, firstDto.daysToEvent(), "Days to event should match");
        assertEquals(Instant.parse("2026-03-17T12:00:00Z"), firstDto.recordedAt(),
                "Recorded at should match");

        PriceHistoryDto secondDto = result.get(1);
        assertEquals(10L, secondDto.id(), "Second entry ID should match");
        assertEquals(new BigDecimal("120.00"), secondDto.price(), "Second entry price should match");
        assertEquals(14, secondDto.daysToEvent(), "Second entry days to event should match");
    }

    @Test
    @DisplayName("getByEventSlug - given non-existent slug - throws ResourceNotFoundException")
    void getByEventSlug_givenNonExistentSlug_throwsResourceNotFoundException() {
        when(eventRepository.findBySlug("non-existent")).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> priceHistoryService.getByEventSlug("non-existent"),
                "Should throw ResourceNotFoundException for unknown slug");

        assertTrue(exception.getMessage().contains("Event"),
                "Exception message should reference Event");
        verifyNoInteractions(priceHistoryRepository);
    }

    @Test
    @DisplayName("getByEventSlug - given slug with no history - returns empty list")
    void getByEventSlug_givenSlugWithNoHistory_returnsEmptyList() {
        when(eventRepository.findBySlug("test-event")).thenReturn(Optional.of(testEvent));
        when(priceHistoryRepository.findByEventIdOrderByRecordedAtDesc(1L))
                .thenReturn(List.of());

        List<PriceHistoryDto> result = priceHistoryService.getByEventSlug("test-event");

        assertNotNull(result, "Result should not be null");
        assertTrue(result.isEmpty(), "Should return empty list when no history exists");
    }

    @Test
    @DisplayName("getByEventId - given valid ID - returns history mapped to DTOs")
    void getByEventId_givenValidId_returnsHistory() {
        when(priceHistoryRepository.findByEventIdOrderByRecordedAtDesc(1L))
                .thenReturn(List.of(historyEntry2, historyEntry1));

        List<PriceHistoryDto> result = priceHistoryService.getByEventId(1L);

        assertNotNull(result, "Result should not be null");
        assertEquals(2, result.size(), "Should return two history entries");

        verify(priceHistoryRepository).findByEventIdOrderByRecordedAtDesc(1L);

        PriceHistoryDto firstDto = result.get(0);
        assertEquals(11L, firstDto.id(), "First entry ID should match");
        assertEquals(1L, firstDto.eventId(), "Event ID should match");
        assertEquals(new BigDecimal("135.00"), firstDto.price(), "Price should match");
        assertEquals(7, firstDto.daysToEvent(), "Days to event should match");
    }

    @Test
    @DisplayName("getByEventId - given ID with no history - returns empty list")
    void getByEventId_givenIdWithNoHistory_returnsEmptyList() {
        when(priceHistoryRepository.findByEventIdOrderByRecordedAtDesc(999L))
                .thenReturn(List.of());

        List<PriceHistoryDto> result = priceHistoryService.getByEventId(999L);

        assertNotNull(result, "Result should not be null");
        assertTrue(result.isEmpty(), "Should return empty list when no history exists");
        verify(priceHistoryRepository).findByEventIdOrderByRecordedAtDesc(999L);
    }

    @Test
    @DisplayName("getByEventId - given valid ID - does not query event repository")
    void getByEventId_givenValidId_doesNotQueryEventRepository() {
        when(priceHistoryRepository.findByEventIdOrderByRecordedAtDesc(1L))
                .thenReturn(List.of(historyEntry1));

        priceHistoryService.getByEventId(1L);

        verifyNoInteractions(eventRepository);
    }
}
