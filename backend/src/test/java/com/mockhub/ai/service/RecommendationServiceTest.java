package com.mockhub.ai.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClient.CallResponseSpec;
import org.springframework.ai.chat.client.ChatClient.ChatClientRequestSpec;

import com.mockhub.ai.dto.RecommendationDto;
import com.mockhub.eval.dto.EvalContext;
import com.mockhub.eval.dto.EvalResult;
import com.mockhub.eval.dto.EvalSummary;
import com.mockhub.eval.service.EvalRunner;
import com.mockhub.event.entity.Category;
import com.mockhub.event.entity.Event;
import com.mockhub.event.repository.EventRepository;
import com.mockhub.venue.entity.Venue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecommendationServiceTest {

    @Mock
    private ChatClient chatClient;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private EvalRunner evalRunner;

    @Mock
    private ChatClientRequestSpec requestSpec;

    @Mock
    private CallResponseSpec callResponseSpec;

    private RecommendationService recommendationService;

    @BeforeEach
    void setUp() {
        recommendationService = new RecommendationService(chatClient, eventRepository, evalRunner);
    }

    private Event createTestEvent(Long id, String name, String slug, String venueName, String city) {
        Event event = new Event();
        event.setId(id);
        event.setName(name);
        event.setSlug(slug);
        event.setMinPrice(new BigDecimal("75.00"));
        event.setEventDate(Instant.now().plus(30, ChronoUnit.DAYS));

        Venue venue = new Venue();
        venue.setName(venueName);
        venue.setCity(city);
        event.setVenue(venue);

        Category category = new Category();
        category.setName("Concert");
        event.setCategory(category);

        return event;
    }

    private void stubEvalRunnerPassing() {
        when(evalRunner.evaluate(any(EvalContext.class)))
                .thenReturn(new EvalSummary(java.util.List.of(EvalResult.pass("test"))));
    }

    @Test
    @DisplayName("getRecommendations - given available events - returns ranked recommendations")
    void getRecommendations_givenAvailableEvents_returnsRankedRecommendations() {
        List<Event> events = List.of(
                createTestEvent(1L, "Rock Festival", "rock-festival", "MSG", "New York"),
                createTestEvent(2L, "Jazz Night", "jazz-night", "Blue Note", "New York"),
                createTestEvent(3L, "Symphony", "symphony", "Carnegie Hall", "New York")
        );
        when(eventRepository.findFeaturedEvents()).thenReturn(events);

        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.content()).thenReturn(
                """
                [
                  {"eventId": 1, "relevanceScore": 0.95, "reason": "Popular rock event with high demand"},
                  {"eventId": 2, "relevanceScore": 0.80, "reason": "Intimate jazz venue experience"},
                  {"eventId": 3, "relevanceScore": 0.70, "reason": "Classical music in a legendary hall"}
                ]
                """);

        stubEvalRunnerPassing();

        List<RecommendationDto> recommendations = recommendationService.getRecommendations();

        assertNotNull(recommendations);
        assertFalse(recommendations.isEmpty(), "Should return at least one recommendation");
        verify(evalRunner).evaluate(any(EvalContext.class));
    }

    @Test
    @DisplayName("getRecommendations - given no events - returns empty list")
    void getRecommendations_givenNoEvents_returnsEmptyList() {
        when(eventRepository.findFeaturedEvents()).thenReturn(List.of());

        List<RecommendationDto> recommendations = recommendationService.getRecommendations();

        assertNotNull(recommendations);
        assertEquals(0, recommendations.size());
    }
}
