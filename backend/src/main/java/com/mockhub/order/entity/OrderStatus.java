package com.mockhub.order.entity;

import java.util.Set;

/**
 * Order lifecycle states with explicit transition rules.
 *
 * <pre>{@code
 * stateDiagram-v2
 *     [*] --> PENDING
 *     PENDING --> CONFIRMED : confirm
 *     PENDING --> FAILED : fail
 *     CONFIRMED --> CANCELLED : cancel
 *     FAILED --> [*]
 *     CANCELLED --> [*]
 * }</pre>
 *
 * @see <a href="https://mermaid.js.org/syntax/stateDiagram.html">Mermaid State Diagram</a>
 */
public enum OrderStatus {

    PENDING(Set.of("CONFIRMED", "FAILED")),
    CONFIRMED(Set.of("CANCELLED")),
    FAILED(Set.of()),
    CANCELLED(Set.of());

    private final Set<String> allowedTransitions;

    OrderStatus(Set<String> allowedTransitions) {
        this.allowedTransitions = allowedTransitions;
    }

    /**
     * Returns true if transitioning from this status to {@code target} is allowed.
     */
    public boolean canTransitionTo(OrderStatus target) {
        return allowedTransitions.contains(target.name());
    }

    /**
     * Transitions to the target status, throwing {@link IllegalStateException}
     * if the transition is not allowed. Returns the target status for fluent use.
     */
    public OrderStatus transitionTo(OrderStatus target) {
        if (!canTransitionTo(target)) {
            throw new IllegalStateException(
                    "Cannot transition order from " + this + " to " + target);
        }
        return target;
    }
}
