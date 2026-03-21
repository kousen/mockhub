package com.mockhub.eval.condition;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.mockhub.ai.dto.RecommendationDto;
import com.mockhub.eval.EvalCondition;
import com.mockhub.eval.dto.EvalContext;
import com.mockhub.eval.dto.EvalResult;
import com.mockhub.eval.dto.EvalSeverity;
import com.mockhub.event.entity.Event;
import com.mockhub.event.repository.EventRepository;

@Component
public class RecommendationAvailabilityCondition implements EvalCondition {

    private final EventRepository eventRepository;

    public RecommendationAvailabilityCondition(EventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    @Override
    public String name() {
        return "recommendation-availability";
    }

    @Override
    public boolean appliesTo(EvalContext context) {
        return context.recommendations() != null && !context.recommendations().isEmpty();
    }

    @Override
    public EvalResult evaluate(EvalContext context) {
        List<String> issues = new ArrayList<>();

        for (RecommendationDto recommendation : context.recommendations()) {
            String eventRef = formatEventRef(recommendation);
            Optional<Event> eventOptional = eventRepository.findById(recommendation.eventId());

            if (eventOptional.isEmpty()) {
                issues.add(eventRef + " not found");
                continue;
            }

            Event event = eventOptional.get();

            if (!event.getEventDate().isAfter(Instant.now())) {
                issues.add(eventRef + " has already occurred");
            }

            if (event.getAvailableTickets() <= 0) {
                issues.add(eventRef + " has no available tickets");
            }
        }

        if (!issues.isEmpty()) {
            return EvalResult.fail(name(), EvalSeverity.WARNING,
                    "Recommendation issues: " + String.join("; ", issues));
        }

        return EvalResult.pass(name());
    }

    private String formatEventRef(RecommendationDto recommendation) {
        return "Event ID " + recommendation.eventId() + " (" + recommendation.eventName() + ")";
    }
}
