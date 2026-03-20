package com.mockhub.mcp.tools;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mockhub.ai.dto.PricePredictionDto;
import com.mockhub.ai.service.PricePredictionService;
import com.mockhub.pricing.service.PriceHistoryService;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PricingToolsTest {

    @Mock
    private PriceHistoryService priceHistoryService;

    @Mock
    private PricePredictionService pricePredictionService;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
    }

    @Nested
    @DisplayName("getPriceHistory")
    class GetPriceHistory {

        private PricingTools pricingTools;

        @BeforeEach
        void setUp() {
            pricingTools = new PricingTools(priceHistoryService, pricePredictionService, objectMapper);
        }

        @Test
        @DisplayName("given valid event slug - returns price history JSON")
        void givenValidEventSlug_returnsPriceHistoryJson() {
            when(priceHistoryService.getByEventSlug("rock-festival")).thenReturn(List.of());

            String result = pricingTools.getPriceHistory("rock-festival");

            assertTrue(result.startsWith("["), "Result should be a JSON array");
            verify(priceHistoryService).getByEventSlug("rock-festival");
        }

        @Test
        @DisplayName("given slug with whitespace - strips whitespace before lookup")
        void givenSlugWithWhitespace_stripsWhitespace() {
            when(priceHistoryService.getByEventSlug("rock-festival")).thenReturn(List.of());

            pricingTools.getPriceHistory("  rock-festival  ");

            verify(priceHistoryService).getByEventSlug("rock-festival");
        }

        @Test
        @DisplayName("given null slug - returns error JSON")
        void givenNullSlug_returnsErrorJson() {
            String result = pricingTools.getPriceHistory(null);

            assertTrue(result.contains("\"error\""), "Result should contain error field");
            assertTrue(result.contains("Event slug is required"), "Result should indicate slug is required");
        }

        @Test
        @DisplayName("given blank slug - returns error JSON")
        void givenBlankSlug_returnsErrorJson() {
            String result = pricingTools.getPriceHistory("   ");

            assertTrue(result.contains("\"error\""), "Result should contain error field");
        }

        @Test
        @DisplayName("given service throws exception - returns error JSON")
        void givenServiceThrowsException_returnsErrorJson() {
            when(priceHistoryService.getByEventSlug("bad-slug"))
                    .thenThrow(new RuntimeException("DB error"));

            String result = pricingTools.getPriceHistory("bad-slug");

            assertTrue(result.contains("\"error\""), "Result should contain error field");
            assertTrue(result.contains("Failed to get price history"), "Result should contain failure message");
        }
    }

    @Nested
    @DisplayName("getPricePrediction")
    class GetPricePrediction {

        @Test
        @DisplayName("given valid slug and AI provider active - returns prediction JSON")
        void givenValidSlugAndAiProviderActive_returnsPredictionJson() {
            PricingTools pricingTools = new PricingTools(priceHistoryService, pricePredictionService, objectMapper);
            PricePredictionDto prediction = new PricePredictionDto(
                    "rock-festival", new BigDecimal("75.00"), new BigDecimal("82.00"),
                    "RISING", 0.72, Instant.now());
            when(pricePredictionService.predictPrice("rock-festival")).thenReturn(prediction);

            String result = pricingTools.getPricePrediction("rock-festival");

            assertTrue(result.contains("\"eventSlug\":\"rock-festival\""), "Result should contain event slug");
            assertTrue(result.contains("\"trend\":\"RISING\""), "Result should contain trend");
        }

        @Test
        @DisplayName("given null prediction service - returns error about no AI provider")
        void givenNullPredictionService_returnsErrorAboutNoAiProvider() {
            PricingTools pricingTools = new PricingTools(priceHistoryService, null, objectMapper);

            String result = pricingTools.getPricePrediction("rock-festival");

            assertTrue(result.contains("\"error\""), "Result should contain error field");
            assertTrue(result.contains("no AI provider is active"),
                    "Result should indicate AI provider not available");
        }

        @Test
        @DisplayName("given null slug - returns error JSON")
        void givenNullSlug_returnsErrorJson() {
            PricingTools pricingTools = new PricingTools(priceHistoryService, pricePredictionService, objectMapper);

            String result = pricingTools.getPricePrediction(null);

            assertTrue(result.contains("\"error\""), "Result should contain error field");
            assertTrue(result.contains("Event slug is required"), "Result should indicate slug is required");
        }

        @Test
        @DisplayName("given blank slug - returns error JSON")
        void givenBlankSlug_returnsErrorJson() {
            PricingTools pricingTools = new PricingTools(priceHistoryService, pricePredictionService, objectMapper);

            String result = pricingTools.getPricePrediction("");

            assertTrue(result.contains("\"error\""), "Result should contain error field");
        }

        @Test
        @DisplayName("given service throws exception - returns error JSON")
        void givenServiceThrowsException_returnsErrorJson() {
            PricingTools pricingTools = new PricingTools(priceHistoryService, pricePredictionService, objectMapper);
            when(pricePredictionService.predictPrice("bad-slug"))
                    .thenThrow(new RuntimeException("Prediction failed"));

            String result = pricingTools.getPricePrediction("bad-slug");

            assertTrue(result.contains("\"error\""), "Result should contain error field");
            assertTrue(result.contains("Failed to get price prediction"), "Result should contain failure message");
        }
    }
}
