package com.mockhub.eval.condition;

import java.time.Instant;

import org.springframework.stereotype.Component;

import com.mockhub.eval.EvalCondition;
import com.mockhub.eval.dto.EvalContext;
import com.mockhub.eval.dto.EvalResult;
import com.mockhub.eval.dto.EvalSeverity;
import com.mockhub.event.entity.Event;

@Component
public class EventInFutureCondition implements EvalCondition {

    @Override
    public String name() {
        return "event-in-future";
    }

    @Override
    public boolean appliesTo(EvalContext context) {
        return context.event() != null;
    }

    @Override
    public EvalResult evaluate(EvalContext context) {
        Event event = context.event();

        if (!event.getEventDate().isAfter(Instant.now())) {
            return EvalResult.fail(name(), EvalSeverity.CRITICAL, "Event has already occurred");
        }

        if (!"ACTIVE".equals(event.getStatus())) {
            return EvalResult.fail(name(), EvalSeverity.CRITICAL,
                    "Event status is not ACTIVE: " + event.getStatus());
        }

        return EvalResult.pass(name());
    }
}
