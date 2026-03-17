package com.mockhub.ai.controller;

import java.util.Collections;
import java.util.List;
import java.util.Map;

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
import com.mockhub.common.dto.ErrorResponse;

/**
 * AI stub endpoints that return 501 Not Implemented.
 * Students implement the backing services to bring these to life.
 */
@RestController
@RequestMapping("/api/v1")
public class AiController {

    private static final ErrorResponse NOT_IMPLEMENTED = new ErrorResponse(
            HttpStatus.NOT_IMPLEMENTED.value(),
            "Not Implemented",
            "This AI feature is not yet implemented. "
                    + "Students will implement this as part of their AI coursework."
    );

    @GetMapping("/recommendations")
    public ResponseEntity<List<RecommendationDto>> getRecommendations() {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(Collections.emptyList());
    }

    @PostMapping("/search/natural-language")
    public ResponseEntity<Map<String, Object>> naturalLanguageSearch(
            @RequestBody Map<String, String> request) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED)
                .body(Map.of(
                        "error", NOT_IMPLEMENTED.error(),
                        "message", NOT_IMPLEMENTED.message()
                ));
    }

    @PostMapping("/chat")
    public ResponseEntity<ChatResponse> chat(@Valid @RequestBody ChatRequest request) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }

    @GetMapping("/events/{slug}/predicted-price")
    public ResponseEntity<PricePredictionDto> predictedPrice(@PathVariable String slug) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }
}
