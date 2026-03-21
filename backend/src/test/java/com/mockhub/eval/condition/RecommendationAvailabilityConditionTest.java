package com.mockhub.eval.condition;

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

import com.mockhub.ai.dto.RecommendationDto;
import com.mockhub.eval.dto.EvalContext;
import com.mockhub.eval.dto.EvalResult;
import com.mockhub.eval.dto.EvalSeverity;
import com.mockhub.event.entity.Event;
import com.mockhub.event.repository.EventRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("RecommendationAvailabilityCondition")
class RecommendationAvailabilityConditionTest {

    @Mock
    private EventRepository eventRepository;

    private RecommendationAvailabilityCondition condition;

    @BeforeEach
    void setUp() {
        condition = new RecommendationAvailabilityCondition(eventRepository);
    }

    @Test
    @DisplayName("name returns recommendation-availability")
    void name_always_returnsRecommendationAvailability() {
        assertThat(condition.name()).isEqualTo("recommendation-availability");
    }

    @Test
    @DisplayName("appliesTo returns true when recommendations are present and non-empty")
    void appliesTo_givenNonEmptyRecommendations_returnsTrue() {
        RecommendationDto recommendation = createRecommendation(1L, "Concert");
        EvalContext context = EvalContext.forRecommendations(List.of(recommendation));

        assertThat(condition.appliesTo(context)).isTrue();
    }

    @Test
    @DisplayName("appliesTo returns false when recommendations are null")
    void appliesTo_givenNullRecommendations_returnsFalse() {
        EvalContext context = EvalContext.forRecommendations(null);

        assertThat(condition.appliesTo(context)).isFalse();
    }

    @Test
    @DisplayName("appliesTo returns false when recommendations list is empty")
    void appliesTo_givenEmptyRecommendations_returnsFalse() {
        EvalContext context = EvalContext.forRecommendations(List.of());

        assertThat(condition.appliesTo(context)).isFalse();
    }

    @Test
    @DisplayName("evaluate passes when all recommendations have valid future events with tickets")
    void evaluate_givenAllValidRecommendations_passes() {
        RecommendationDto rec1 = createRecommendation(1L, "Concert A");
        RecommendationDto rec2 = createRecommendation(2L, "Concert B");

        Event event1 = createFutureEventWithTickets(100);
        Event event2 = createFutureEventWithTickets(50);

        when(eventRepository.findById(1L)).thenReturn(Optional.of(event1));
        when(eventRepository.findById(2L)).thenReturn(Optional.of(event2));

        EvalContext context = EvalContext.forRecommendations(List.of(rec1, rec2));

        EvalResult result = condition.evaluate(context);

        assertThat(result.passed()).isTrue();
        assertThat(result.conditionName()).isEqualTo("recommendation-availability");
    }

    @Test
    @DisplayName("evaluate fails with WARNING when one recommendation has a past event")
    void evaluate_givenPastEvent_failsWarning() {
        RecommendationDto rec1 = createRecommendation(1L, "Past Concert");

        Event pastEvent = new Event();
        pastEvent.setEventDate(Instant.now().minus(1, ChronoUnit.DAYS));
        pastEvent.setAvailableTickets(100);

        when(eventRepository.findById(1L)).thenReturn(Optional.of(pastEvent));

        EvalContext context = EvalContext.forRecommendations(List.of(rec1));

        EvalResult result = condition.evaluate(context);

        assertThat(result.passed()).isFalse();
        assertThat(result.severity()).isEqualTo(EvalSeverity.WARNING);
        assertThat(result.message()).contains("Past Concert");
        assertThat(result.message()).contains("already occurred");
    }

    @Test
    @DisplayName("evaluate fails with WARNING when one recommendation has a sold-out event")
    void evaluate_givenSoldOutEvent_failsWarning() {
        RecommendationDto rec1 = createRecommendation(1L, "Sold Out Concert");

        Event soldOutEvent = new Event();
        soldOutEvent.setEventDate(Instant.now().plus(30, ChronoUnit.DAYS));
        soldOutEvent.setAvailableTickets(0);

        when(eventRepository.findById(1L)).thenReturn(Optional.of(soldOutEvent));

        EvalContext context = EvalContext.forRecommendations(List.of(rec1));

        EvalResult result = condition.evaluate(context);

        assertThat(result.passed()).isFalse();
        assertThat(result.severity()).isEqualTo(EvalSeverity.WARNING);
        assertThat(result.message()).contains("Sold Out Concert");
        assertThat(result.message()).contains("no available tickets");
    }

    @Test
    @DisplayName("evaluate fails with WARNING when recommendation references non-existent event")
    void evaluate_givenNonExistentEvent_failsWarning() {
        RecommendationDto rec1 = createRecommendation(999L, "Ghost Concert");

        when(eventRepository.findById(999L)).thenReturn(Optional.empty());

        EvalContext context = EvalContext.forRecommendations(List.of(rec1));

        EvalResult result = condition.evaluate(context);

        assertThat(result.passed()).isFalse();
        assertThat(result.severity()).isEqualTo(EvalSeverity.WARNING);
        assertThat(result.message()).contains("Ghost Concert");
        assertThat(result.message()).contains("not found");
    }

    private RecommendationDto createRecommendation(Long eventId, String eventName) {
        return new RecommendationDto(
                eventId,
                eventName,
                eventName.toLowerCase().replace(" ", "-"),
                "Test Venue",
                "Test City",
                Instant.now().plus(30, ChronoUnit.DAYS),
                new BigDecimal("50.00"),
                0.85,
                "Great event"
        );
    }

    private Event createFutureEventWithTickets(int availableTickets) {
        Event event = new Event();
        event.setEventDate(Instant.now().plus(30, ChronoUnit.DAYS));
        event.setAvailableTickets(availableTickets);
        return event;
    }
}
