package com.mockhub.ai.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClient.CallResponseSpec;
import org.springframework.ai.chat.client.ChatClient.ChatClientRequestSpec;

import com.mockhub.ai.dto.PricePredictionDto;
import com.mockhub.common.exception.ResourceNotFoundException;
import com.mockhub.event.entity.Event;
import com.mockhub.event.repository.EventRepository;
import com.mockhub.pricing.entity.PriceHistory;
import com.mockhub.pricing.repository.PriceHistoryRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PricePredictionServiceTest {

    @Mock
    private ChatClient chatClient;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private PriceHistoryRepository priceHistoryRepository;

    @Mock
    private ChatClientRequestSpec requestSpec;

    @Mock
    private CallResponseSpec callResponseSpec;

    private PricePredictionService pricePredictionService;
    private Event testEvent;

    @BeforeEach
    void setUp() {
        pricePredictionService = new PricePredictionService(chatClient, eventRepository, priceHistoryRepository);

        testEvent = new Event();
        testEvent.setId(1L);
        testEvent.setName("Rock Festival 2026");
        testEvent.setSlug("rock-festival-2026");
        testEvent.setMinPrice(new BigDecimal("75.00"));
        testEvent.setMaxPrice(new BigDecimal("200.00"));
        testEvent.setEventDate(Instant.now().plus(30, ChronoUnit.DAYS));
    }

    @Test
    @DisplayName("predictPrice - given existing event with price history - returns prediction")
    void predictPrice_givenExistingEventWithHistory_returnsPrediction() {
        when(eventRepository.findBySlug("rock-festival-2026")).thenReturn(Optional.of(testEvent));

        PriceHistory ph1 = new PriceHistory();
        ph1.setPrice(new BigDecimal("70.00"));
        ph1.setDaysToEvent(45);
        ph1.setRecordedAt(Instant.now().minus(15, ChronoUnit.DAYS));
        PriceHistory ph2 = new PriceHistory();
        ph2.setPrice(new BigDecimal("75.00"));
        ph2.setDaysToEvent(30);
        ph2.setRecordedAt(Instant.now());

        when(priceHistoryRepository.findByEventIdOrderByRecordedAtDesc(1L))
                .thenReturn(List.of(ph2, ph1));

        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.content()).thenReturn(
                """
                {"predictedPrice": 82.00, "trend": "RISING", "confidence": 0.72}
                """);

        PricePredictionDto prediction = pricePredictionService.predictPrice("rock-festival-2026");

        assertNotNull(prediction);
        assertEquals("rock-festival-2026", prediction.eventSlug());
        assertEquals(new BigDecimal("75.00"), prediction.currentPrice());
        assertNotNull(prediction.predictedPrice());
        assertNotNull(prediction.trend());
        assertTrue(prediction.confidence() >= 0.0 && prediction.confidence() <= 1.0);
        assertNotNull(prediction.predictedAt());
    }

    @Test
    @DisplayName("predictPrice - given nonexistent event - throws ResourceNotFoundException")
    void predictPrice_givenNonexistentEvent_throwsResourceNotFoundException() {
        when(eventRepository.findBySlug("nonexistent")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> pricePredictionService.predictPrice("nonexistent"));
    }
}
