package com.mockhub.ai.controller;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.mockhub.ai.dto.ChatRequest;
import com.mockhub.ai.dto.ChatResponse;
import com.mockhub.ai.dto.PricePredictionDto;
import com.mockhub.ai.dto.RecommendationDto;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@DisplayName("AiController - service unavailable (503) paths")
class AiControllerServiceUnavailableTest {

    private AiController controller;

    @BeforeEach
    void setUp() {
        controller = new AiController(Optional.empty(), Optional.empty(), Optional.empty());
    }

    @Test
    @DisplayName("chat - given no ChatService configured - returns 503 SERVICE_UNAVAILABLE")
    void chat_givenNoChatServiceConfigured_returns503() {
        ChatRequest request = new ChatRequest("What concerts are available?", null);

        ResponseEntity<ChatResponse> response = controller.chat(request);

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode(),
                "Should return 503 when ChatService is not configured");
        assertNull(response.getBody(), "Response body should be null for 503");
    }

    @Test
    @DisplayName("getRecommendations - given no RecommendationService configured - returns 503 SERVICE_UNAVAILABLE")
    void getRecommendations_givenNoRecommendationServiceConfigured_returns503() {
        ResponseEntity<List<RecommendationDto>> response = controller.getRecommendations();

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode(),
                "Should return 503 when RecommendationService is not configured");
        assertNull(response.getBody(), "Response body should be null for 503");
    }

    @Test
    @DisplayName("predictedPrice - given no PricePredictionService configured - returns 503 SERVICE_UNAVAILABLE")
    void predictedPrice_givenNoPricePredictionServiceConfigured_returns503() {
        ResponseEntity<PricePredictionDto> response = controller.predictedPrice("rock-festival");

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode(),
                "Should return 503 when PricePredictionService is not configured");
        assertNull(response.getBody(), "Response body should be null for 503");
    }
}
