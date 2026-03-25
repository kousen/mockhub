package com.mockhub.ai.controller;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.mockhub.ai.dto.ChatResponse;
import com.mockhub.ai.dto.PricePredictionDto;
import com.mockhub.ai.dto.RecommendationDto;
import com.mockhub.ai.service.ChatService;
import com.mockhub.ai.service.PricePredictionService;
import com.mockhub.ai.service.RecommendationService;
import com.mockhub.auth.security.JwtAuthenticationFilter;
import com.mockhub.auth.security.JwtTokenProvider;
import com.mockhub.auth.security.UserDetailsServiceImpl;
import com.mockhub.config.SecurityConfig;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AiController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class AiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ChatService chatService;

    @MockitoBean
    private RecommendationService recommendationService;

    @MockitoBean
    private PricePredictionService pricePredictionService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private UserDetailsServiceImpl userDetailsService;

    @Nested
    @DisplayName("POST /api/v1/chat")
    class ChatEndpoint {

        @Test
        @DisplayName("given valid message - returns 200 with AI response")
        void givenValidMessage_returns200() throws Exception {
            ChatResponse response = new ChatResponse(null, "Here are some events...", Instant.now());
            when(chatService.chat(any(), any())).thenReturn(response);

            mockMvc.perform(post("/api/v1/chat")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"message": "What concerts are this weekend?"}
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Here are some events..."));
        }

        @Test
        @DisplayName("given blank message - returns 400")
        void givenBlankMessage_returns400() throws Exception {
            mockMvc.perform(post("/api/v1/chat")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"message": ""}
                                    """))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/recommendations")
    class RecommendationsEndpoint {

        @Test
        @DisplayName("returns 200 with recommendation list")
        void returns200WithRecommendations() throws Exception {
            RecommendationDto rec = new RecommendationDto(
                    1L, "Rock Festival", "rock-festival", "MSG", "New York",
                    Instant.now(), new BigDecimal("75.00"), 0.95, "Popular event");
            when(recommendationService.getRecommendations(any())).thenReturn(List.of(rec));

            mockMvc.perform(get("/api/v1/recommendations"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].eventName").value("Rock Festival"))
                    .andExpect(jsonPath("$[0].relevanceScore").value(0.95));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/events/{slug}/predicted-price")
    class PricePredictionEndpoint {

        @Test
        @DisplayName("given existing event - returns 200 with prediction")
        void givenExistingEvent_returns200() throws Exception {
            PricePredictionDto prediction = new PricePredictionDto(
                    "rock-festival", new BigDecimal("75.00"), new BigDecimal("82.00"),
                    "RISING", 0.72, Instant.now());
            when(pricePredictionService.predictPrice("rock-festival")).thenReturn(prediction);

            mockMvc.perform(get("/api/v1/events/rock-festival/predicted-price"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.eventSlug").value("rock-festival"))
                    .andExpect(jsonPath("$.trend").value("RISING"));
        }
    }
}
