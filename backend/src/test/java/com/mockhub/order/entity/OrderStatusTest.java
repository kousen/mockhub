package com.mockhub.order.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class OrderStatusTest {

    // ── Valid transitions ──────────────────────────────────────────────

    @Test
    void pending_canTransitionToConfirmed() {
        assertTrue(OrderStatus.PENDING.canTransitionTo(OrderStatus.CONFIRMED));
    }

    @Test
    void pending_canTransitionToFailed() {
        assertTrue(OrderStatus.PENDING.canTransitionTo(OrderStatus.FAILED));
    }

    @Test
    void confirmed_canTransitionToCancelled() {
        assertTrue(OrderStatus.CONFIRMED.canTransitionTo(OrderStatus.CANCELLED));
    }

    // ── Invalid transitions ───────────────────────────────────────────

    @Test
    void pending_cannotTransitionToCancelled() {
        assertFalse(OrderStatus.PENDING.canTransitionTo(OrderStatus.CANCELLED));
    }

    @Test
    void confirmed_cannotTransitionToFailed() {
        assertFalse(OrderStatus.CONFIRMED.canTransitionTo(OrderStatus.FAILED));
    }

    @Test
    void confirmed_cannotTransitionToPending() {
        assertFalse(OrderStatus.CONFIRMED.canTransitionTo(OrderStatus.PENDING));
    }

    @Test
    void failed_isTerminal() {
        assertFalse(OrderStatus.FAILED.canTransitionTo(OrderStatus.PENDING));
        assertFalse(OrderStatus.FAILED.canTransitionTo(OrderStatus.CONFIRMED));
        assertFalse(OrderStatus.FAILED.canTransitionTo(OrderStatus.CANCELLED));
    }

    @Test
    void cancelled_isTerminal() {
        assertFalse(OrderStatus.CANCELLED.canTransitionTo(OrderStatus.PENDING));
        assertFalse(OrderStatus.CANCELLED.canTransitionTo(OrderStatus.CONFIRMED));
        assertFalse(OrderStatus.CANCELLED.canTransitionTo(OrderStatus.FAILED));
    }

    // ── transitionTo() ────────────────────────────────────────────────

    @Test
    void transitionTo_returnsTargetOnValidTransition() {
        OrderStatus result = OrderStatus.PENDING.transitionTo(OrderStatus.CONFIRMED);
        assertEquals(OrderStatus.CONFIRMED, result);
    }

    @Test
    void transitionTo_throwsOnInvalidTransition() {
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> OrderStatus.FAILED.transitionTo(OrderStatus.CONFIRMED)
        );
        assertEquals("Cannot transition order from FAILED to CONFIRMED", exception.getMessage());
    }

    // ── Self-transitions are not allowed ──────────────────────────────

    @Test
    void pending_cannotTransitionToSelf() {
        assertFalse(OrderStatus.PENDING.canTransitionTo(OrderStatus.PENDING));
    }

    @Test
    void confirmed_cannotTransitionToSelf() {
        assertFalse(OrderStatus.CONFIRMED.canTransitionTo(OrderStatus.CONFIRMED));
    }

    // ── Order entity integration ──────────────────────────────────────

    @Test
    void order_transitionTo_updatesStatus() {
        Order order = new Order();
        order.setStatus(OrderStatus.PENDING);

        order.transitionTo(OrderStatus.CONFIRMED);

        assertEquals(OrderStatus.CONFIRMED, order.getStatus());
    }

    @Test
    void order_transitionTo_throwsOnInvalidTransition() {
        Order order = new Order();
        order.setStatus(OrderStatus.FAILED);

        assertThrows(
                IllegalStateException.class,
                () -> order.transitionTo(OrderStatus.CONFIRMED)
        );
        assertEquals(OrderStatus.FAILED, order.getStatus());
    }

    // ── Complete lifecycle ─────────────────────────────────────────────

    @Test
    void fullLifecycle_pendingToConfirmedToCancelled() {
        Order order = new Order();
        order.setStatus(OrderStatus.PENDING);

        order.transitionTo(OrderStatus.CONFIRMED);
        assertEquals(OrderStatus.CONFIRMED, order.getStatus());

        order.transitionTo(OrderStatus.CANCELLED);
        assertEquals(OrderStatus.CANCELLED, order.getStatus());
    }

    @Test
    void fullLifecycle_pendingToFailed() {
        Order order = new Order();
        order.setStatus(OrderStatus.PENDING);

        order.transitionTo(OrderStatus.FAILED);
        assertEquals(OrderStatus.FAILED, order.getStatus());

        // FAILED is terminal
        assertThrows(
                IllegalStateException.class,
                () -> order.transitionTo(OrderStatus.CANCELLED)
        );
    }
}
