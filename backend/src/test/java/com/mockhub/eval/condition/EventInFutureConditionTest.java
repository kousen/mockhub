package com.mockhub.eval.condition;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.mockhub.eval.dto.EvalContext;
import com.mockhub.eval.dto.EvalResult;
import com.mockhub.eval.dto.EvalSeverity;
import com.mockhub.event.entity.Event;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("EventInFutureCondition")
class EventInFutureConditionTest {

    private EventInFutureCondition condition;

    @BeforeEach
    void setUp() {
        condition = new EventInFutureCondition();
    }

    @Test
    @DisplayName("name returns event-in-future")
    void name_always_returnsEventInFuture() {
        assertThat(condition.name()).isEqualTo("event-in-future");
    }

    @Test
    @DisplayName("appliesTo returns true when event is present")
    void appliesTo_givenEventPresent_returnsTrue() {
        Event event = new Event();
        EvalContext context = EvalContext.forEvent(event);

        assertThat(condition.appliesTo(context)).isTrue();
    }

    @Test
    @DisplayName("appliesTo returns false when event is null")
    void appliesTo_givenNoEvent_returnsFalse() {
        EvalContext context = EvalContext.forCart(null);

        assertThat(condition.appliesTo(context)).isFalse();
    }

    @Test
    @DisplayName("evaluate passes for future active event")
    void evaluate_givenFutureActiveEvent_passes() {
        Event event = new Event();
        event.setEventDate(Instant.now().plus(30, ChronoUnit.DAYS));
        event.setStatus("ACTIVE");
        EvalContext context = EvalContext.forEvent(event);

        EvalResult result = condition.evaluate(context);

        assertThat(result.passed()).isTrue();
        assertThat(result.conditionName()).isEqualTo("event-in-future");
    }

    @Test
    @DisplayName("evaluate fails with CRITICAL for past event")
    void evaluate_givenPastEvent_failsCritical() {
        Event event = new Event();
        event.setEventDate(Instant.now().minus(1, ChronoUnit.DAYS));
        event.setStatus("ACTIVE");
        EvalContext context = EvalContext.forEvent(event);

        EvalResult result = condition.evaluate(context);

        assertThat(result.passed()).isFalse();
        assertThat(result.severity()).isEqualTo(EvalSeverity.CRITICAL);
        assertThat(result.message()).isEqualTo("Event has already occurred");
    }

    @Test
    @DisplayName("evaluate fails with CRITICAL for cancelled event")
    void evaluate_givenCancelledEvent_failsCritical() {
        Event event = new Event();
        event.setEventDate(Instant.now().plus(30, ChronoUnit.DAYS));
        event.setStatus("CANCELLED");
        EvalContext context = EvalContext.forEvent(event);

        EvalResult result = condition.evaluate(context);

        assertThat(result.passed()).isFalse();
        assertThat(result.severity()).isEqualTo(EvalSeverity.CRITICAL);
        assertThat(result.message()).isEqualTo("Event status is not ACTIVE: CANCELLED");
    }

    @Test
    @DisplayName("evaluate fails with past-event message when both past and cancelled")
    void evaluate_givenPastAndCancelledEvent_failsWithPastEventMessage() {
        Event event = new Event();
        event.setEventDate(Instant.now().minus(1, ChronoUnit.DAYS));
        event.setStatus("CANCELLED");
        EvalContext context = EvalContext.forEvent(event);

        EvalResult result = condition.evaluate(context);

        assertThat(result.passed()).isFalse();
        assertThat(result.severity()).isEqualTo(EvalSeverity.CRITICAL);
        assertThat(result.message()).isEqualTo("Event has already occurred");
    }
}
