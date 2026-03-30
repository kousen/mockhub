package com.mockhub.ai.controller;

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

import org.springframework.security.core.annotation.AuthenticationPrincipal;

import org.springframework.web.bind.annotation.RequestParam;

import com.mockhub.ai.dto.ChatRequest;
import com.mockhub.ai.dto.ChatResponse;
import com.mockhub.ai.dto.PricePredictionDto;
import com.mockhub.ai.dto.RecommendationsResponse;
import com.mockhub.ai.service.ChatService;
import com.mockhub.ai.service.PricePredictionService;
import com.mockhub.ai.service.RecommendationService;
import com.mockhub.auth.security.SecurityUser;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "AI", description = "AI-powered features: chat, recommendations, price predictions")
public class AiController {

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
    public ResponseEntity<ChatResponse> chat(@Valid @RequestBody ChatRequest request,
                                               @AuthenticationPrincipal SecurityUser securityUser) {
        if (chatService.isEmpty()) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }
        String userEmail = securityUser != null ? securityUser.getEmail() : null;
        return ResponseEntity.ok(chatService.get().chat(request, userEmail));
    }

    @GetMapping("/recommendations")
    @Operation(summary = "Get AI recommendations", description = "AI-ranked event recommendations personalized to user preferences, Spotify listening data, and optional city filter")
    @ApiResponse(responseCode = "200", description = "Recommendations returned")
    @ApiResponse(responseCode = "503", description = "AI provider not configured")
    public ResponseEntity<RecommendationsResponse> getRecommendations(
            @AuthenticationPrincipal SecurityUser securityUser,
            @RequestParam(required = false) String city) {
        if (recommendationService.isEmpty()) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }
        Long userId = securityUser != null ? securityUser.getId() : null;
        return ResponseEntity.ok(recommendationService.get().getRecommendations(userId, city));
    }

    @GetMapping("/events/{slug}/predicted-price")
    @Operation(summary = "Predict ticket price", description = "AI-predicted price trend for an event based on historical data")
    @ApiResponse(responseCode = "200", description = "Price prediction returned")
    @ApiResponse(responseCode = "503", description = "AI provider not configured")
    public ResponseEntity<PricePredictionDto> predictedPrice(@PathVariable String slug) {
        if (pricePredictionService.isEmpty()) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }
        return ResponseEntity.ok(pricePredictionService.get().predictPrice(slug));
    }
}
