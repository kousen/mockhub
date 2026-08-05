package com.mockhub.order.dto;

import java.math.BigDecimal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OrderPricingTest {

    @Test
    @DisplayName("forSubtotal_givenSubtotal_addsTenPercentServiceFee")
    void forSubtotal_givenSubtotal_addsTenPercentServiceFee() {
        OrderPricing pricing = OrderPricing.forSubtotal(new BigDecimal("30.00"));

        assertEquals(new BigDecimal("30.00"), pricing.subtotal());
        assertEquals(new BigDecimal("3.00"), pricing.serviceFee());
        assertEquals(new BigDecimal("33.00"), pricing.total());
    }

    @Test
    @DisplayName("forSubtotal_givenFractionalFee_roundsFeeToCents")
    void forSubtotal_givenFractionalFee_roundsFeeToCents() {
        OrderPricing pricing = OrderPricing.forSubtotal(new BigDecimal("32.15"));

        assertEquals(new BigDecimal("3.22"), pricing.serviceFee());
        assertEquals(new BigDecimal("35.37"), pricing.total());
    }

    @Test
    @DisplayName("totalForSubtotal_givenSubtotalUnderCeiling_exceedsCeilingOnceFeeApplied")
    void totalForSubtotal_givenSubtotalUnderCeiling_exceedsCeilingOnceFeeApplied() {
        // The bug this class exists to prevent: a $32.15 ticket under a $35.00 mandate
        // ceiling charges the buyer $35.37. Authorization must see the larger number.
        BigDecimal ceiling = new BigDecimal("35.00");
        BigDecimal ticketPrice = new BigDecimal("32.15");

        assertEquals(-1, ticketPrice.compareTo(ceiling), "ticket price alone looks affordable");
        assertEquals(1, OrderPricing.totalForSubtotal(ticketPrice).compareTo(ceiling),
                "but the amount actually charged exceeds the ceiling");
    }

    @Test
    @DisplayName("forSubtotal_givenNullSubtotal_throws")
    void forSubtotal_givenNullSubtotal_throws() {
        assertThrows(IllegalArgumentException.class, () -> OrderPricing.forSubtotal(null));
    }
}
