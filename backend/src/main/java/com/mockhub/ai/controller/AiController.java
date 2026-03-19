package com.mockhub.ai.controller;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mockhub.ai.dto.ChatRequest;
import com.mockhub.ai.dto.ChatResponse;
import com.mockhub.ai.dto.PricePredictionDto;
import com.mockhub.ai.dto.RecommendationDto;
import com.mockhub.ai.service.ChatService;
import com.mockhub.ai.service.PricePredictionService;
import com.mockhub.ai.service.RecommendationService;
import com.mockhub.common.dto.ErrorResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "AI", description = "AI-powered features: chat, recommendations, price predictions")
public class AiController {

    private static final ErrorResponse AI_UNAVAILABLE = new ErrorResponse(
            HttpStatus.SERVICE_UNAVAILABLE.value(),
            "Service Unavailable",
            "AI features require an active AI provider profile (ai-anthropic, ai-openai, or ai-ollama)"
    );

    private final Optional<ChatService> chatService;
    private final Optional<RecommendationService> recommendationService;
    private final Optional<PricePredictionService> pricePredictionService;

    public AiController(Optional<ChatService> chatService,
                        Optional<RecommendationService> recommendationService,
                        Optional<PricePredictionService> pricePredictionService) {
        this.chatService = chatService;
        this.recommendationService = recommendationService;
        this.pricePredictionService = pricePredictionService;
    }

    @PostMapping("/chat")
    @Operation(summary = "Chat with AI assistant", description = "Ask questions about events and get AI-powered responses")
    @ApiResponse(responseCode = "200", description = "AI response returned")
    @ApiResponse(responseCode = "503", description = "AI provider not configured")
    public ResponseEntity<?> chat(@Valid @RequestBody ChatRequest request) {
        if (chatService.isEmpty()) {
            return unavailable();
        }
        return ResponseEntity.ok(chatService.get().chat(request));
    }

    @GetMapping("/recommendations")
    @Operation(summary = "Get AI recommendations", description = "AI-ranked event recommendations based on popularity and appeal")
    @ApiResponse(responseCode = "200", description = "Recommendations returned")
    @ApiResponse(responseCode = "503", description = "AI provider not configured")
    public ResponseEntity<?> getRecommendations() {
        if (recommendationService.isEmpty()) {
            return unavailable();
        }
        return ResponseEntity.ok(recommendationService.get().getRecommendations());
    }

    @GetMapping("/events/{slug}/predicted-price")
    @Operation(summary = "Predict ticket price", description = "AI-predicted price trend for an event based on historical data")
    @ApiResponse(responseCode = "200", description = "Price prediction returned")
    @ApiResponse(responseCode = "503", description = "AI provider not configured")
    public ResponseEntity<?> predictedPrice(@PathVariable String slug) {
        if (pricePredictionService.isEmpty()) {
            return unavailable();
        }
        return ResponseEntity.ok(pricePredictionService.get().predictPrice(slug));
    }

    private ResponseEntity<ErrorResponse> unavailable() {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(AI_UNAVAILABLE);
    }
}
