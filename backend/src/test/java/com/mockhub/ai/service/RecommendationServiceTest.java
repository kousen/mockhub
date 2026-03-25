package com.mockhub.ai.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
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
import com.mockhub.event.entity.Tag;
import com.mockhub.event.repository.EventRepository;
import com.mockhub.favorite.entity.Favorite;
import com.mockhub.favorite.repository.FavoriteRepository;
import com.mockhub.order.repository.OrderItemRepository;
import com.mockhub.venue.entity.Venue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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
    private FavoriteRepository favoriteRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private ChatClientRequestSpec requestSpec;

    @Mock
    private CallResponseSpec callResponseSpec;

    @Captor
    private ArgumentCaptor<String> promptCaptor;

    private RecommendationService recommendationService;

    @BeforeEach
    void setUp() {
        recommendationService = new RecommendationService(
                chatClient, eventRepository, evalRunner, favoriteRepository, orderItemRepository);
    }

    private Event createTestEvent(Long id, String name, String slug, String venueName,
                                   String city, String categoryName, String artistName) {
        Event event = new Event();
        event.setId(id);
        event.setName(name);
        event.setSlug(slug);
        event.setArtistName(artistName);
        event.setMinPrice(new BigDecimal("75.00"));
        event.setEventDate(Instant.now().plus(30, ChronoUnit.DAYS));

        Venue venue = new Venue();
        venue.setName(venueName);
        venue.setCity(city);
        event.setVenue(venue);

        Category category = new Category();
        category.setName(categoryName);
        event.setCategory(category);

        return event;
    }

    private Event createTestEvent(Long id, String name, String slug, String venueName, String city) {
        return createTestEvent(id, name, slug, venueName, city, "Concert", null);
    }

    private Favorite createFavorite(Event event) {
        Favorite favorite = new Favorite();
        favorite.setEvent(event);
        return favorite;
    }

    private void stubChatClient(String aiResponse) {
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.content()).thenReturn(aiResponse);
    }

    private void stubEvalRunnerPassing() {
        when(evalRunner.evaluate(any(EvalContext.class)))
                .thenReturn(new EvalSummary(List.of(EvalResult.pass("test"))));
    }

    private static final String AI_RESPONSE = """
            [
              {"eventId": 1, "relevanceScore": 0.95, "reason": "Popular rock event with high demand"},
              {"eventId": 2, "relevanceScore": 0.80, "reason": "Intimate jazz venue experience"},
              {"eventId": 3, "relevanceScore": 0.70, "reason": "Classical music in a legendary hall"}
            ]
            """;

    @Test
    @DisplayName("getRecommendations - given null userId - uses generic prompt")
    void getRecommendations_givenNullUserId_usesGenericPrompt() {
        List<Event> events = List.of(
                createTestEvent(1L, "Rock Festival", "rock-festival", "MSG", "New York"),
                createTestEvent(2L, "Jazz Night", "jazz-night", "Blue Note", "New York"),
                createTestEvent(3L, "Symphony", "symphony", "Carnegie Hall", "New York")
        );
        when(eventRepository.findFeaturedEvents()).thenReturn(events);
        stubChatClient(AI_RESPONSE);
        stubEvalRunnerPassing();

        List<RecommendationDto> recommendations = recommendationService.getRecommendations(null);

        assertNotNull(recommendations);
        assertFalse(recommendations.isEmpty());
        verify(requestSpec).user(promptCaptor.capture());
        String prompt = promptCaptor.getValue();
        assertTrue(prompt.contains("general appeal"), "Generic prompt should mention 'general appeal'");
        assertFalse(prompt.contains("user's favorite"), "Generic prompt should not mention user preferences");

        verifyNoInteractions(favoriteRepository);
        verifyNoInteractions(orderItemRepository);
    }

    @Test
    @DisplayName("getRecommendations - given user with favorites - includes favorite context in prompt")
    void getRecommendations_givenUserWithFavorites_includesFavoriteContextInPrompt() {
        List<Event> featuredEvents = List.of(
                createTestEvent(1L, "Rock Festival", "rock-festival", "MSG", "New York"),
                createTestEvent(2L, "Jazz Night", "jazz-night", "Blue Note", "New York"),
                createTestEvent(3L, "Symphony", "symphony", "Carnegie Hall", "New York")
        );
        when(eventRepository.findFeaturedEvents()).thenReturn(featuredEvents);

        Event favEvent = createTestEvent(10L, "Blues Night", "blues-night", "Jazz Club", "Chicago", "Concerts", "B.B. King");
        when(favoriteRepository.findByUserIdWithEventDetails(42L)).thenReturn(List.of(createFavorite(favEvent)));
        when(orderItemRepository.findDistinctPurchasedEventsByUserId(42L)).thenReturn(List.of());

        stubChatClient(AI_RESPONSE);
        stubEvalRunnerPassing();

        List<RecommendationDto> recommendations = recommendationService.getRecommendations(42L);

        assertNotNull(recommendations);
        verify(requestSpec).user(promptCaptor.capture());
        String prompt = promptCaptor.getValue();
        assertTrue(prompt.contains("Concerts"), "Prompt should include favorite category");
        assertTrue(prompt.contains("B.B. King"), "Prompt should include favorite artist");
        assertTrue(prompt.contains("Chicago"), "Prompt should include favorite city");
    }

    @Test
    @DisplayName("getRecommendations - given user with purchase history - includes purchase context in prompt")
    void getRecommendations_givenUserWithPurchaseHistory_includesPurchaseContextInPrompt() {
        List<Event> featuredEvents = List.of(
                createTestEvent(1L, "Rock Festival", "rock-festival", "MSG", "New York")
        );
        when(eventRepository.findFeaturedEvents()).thenReturn(featuredEvents);

        Event purchasedEvent = createTestEvent(20L, "Yo-Yo Ma", "yo-yo-ma", "Symphony Hall", "Boston", "Classical", "Yo-Yo Ma");
        when(favoriteRepository.findByUserIdWithEventDetails(42L)).thenReturn(List.of());
        when(orderItemRepository.findDistinctPurchasedEventsByUserId(42L)).thenReturn(List.of(purchasedEvent));

        stubChatClient("""
                [{"eventId": 1, "relevanceScore": 0.90, "reason": "Great show"}]
                """);
        stubEvalRunnerPassing();

        List<RecommendationDto> recommendations = recommendationService.getRecommendations(42L);

        assertNotNull(recommendations);
        verify(requestSpec).user(promptCaptor.capture());
        String prompt = promptCaptor.getValue();
        assertTrue(prompt.contains("Classical"), "Prompt should include purchased event category");
        assertTrue(prompt.contains("Yo-Yo Ma"), "Prompt should include purchased event artist");
        assertTrue(prompt.contains("Boston"), "Prompt should include purchased event city");
    }

    @Test
    @DisplayName("getRecommendations - given user with both signals - combines context")
    void getRecommendations_givenUserWithBothSignals_combinesContext() {
        List<Event> featuredEvents = List.of(
                createTestEvent(1L, "Rock Festival", "rock-festival", "MSG", "New York")
        );
        when(eventRepository.findFeaturedEvents()).thenReturn(featuredEvents);

        Event favEvent = createTestEvent(10L, "Jazz Night", "jazz-night", "Jazz Club", "Chicago", "Concerts", "Miles Davis");
        Event purchasedEvent = createTestEvent(20L, "Yo-Yo Ma", "yo-yo-ma", "Symphony Hall", "Boston", "Classical", "Yo-Yo Ma");
        when(favoriteRepository.findByUserIdWithEventDetails(42L)).thenReturn(List.of(createFavorite(favEvent)));
        when(orderItemRepository.findDistinctPurchasedEventsByUserId(42L)).thenReturn(List.of(purchasedEvent));

        stubChatClient("""
                [{"eventId": 1, "relevanceScore": 0.90, "reason": "Great show"}]
                """);
        stubEvalRunnerPassing();

        List<RecommendationDto> recommendations = recommendationService.getRecommendations(42L);

        assertNotNull(recommendations);
        verify(requestSpec).user(promptCaptor.capture());
        String prompt = promptCaptor.getValue();
        assertTrue(prompt.contains("Miles Davis"), "Prompt should include favorite artist");
        assertTrue(prompt.contains("Yo-Yo Ma"), "Prompt should include purchased artist");
        assertTrue(prompt.contains("relevance to this user"), "Prompt should ask for personalized ranking");
    }

    @Test
    @DisplayName("getRecommendations - given user with no signals - falls back to generic prompt")
    void getRecommendations_givenUserWithNoSignals_fallsToGenericPrompt() {
        List<Event> events = List.of(
                createTestEvent(1L, "Rock Festival", "rock-festival", "MSG", "New York")
        );
        when(eventRepository.findFeaturedEvents()).thenReturn(events);
        when(favoriteRepository.findByUserIdWithEventDetails(42L)).thenReturn(List.of());
        when(orderItemRepository.findDistinctPurchasedEventsByUserId(42L)).thenReturn(List.of());

        stubChatClient("""
                [{"eventId": 1, "relevanceScore": 0.90, "reason": "Great show"}]
                """);
        stubEvalRunnerPassing();

        List<RecommendationDto> recommendations = recommendationService.getRecommendations(42L);

        assertNotNull(recommendations);
        verify(requestSpec).user(promptCaptor.capture());
        String prompt = promptCaptor.getValue();
        assertTrue(prompt.contains("general appeal"), "Should fall back to generic prompt when no user signals");
    }

    @Test
    @DisplayName("getRecommendations - given no events - returns empty list")
    void getRecommendations_givenNoEvents_returnsEmptyList() {
        when(eventRepository.findFeaturedEvents()).thenReturn(List.of());

        List<RecommendationDto> recommendations = recommendationService.getRecommendations(null);

        assertNotNull(recommendations);
        assertEquals(0, recommendations.size());
    }
}
