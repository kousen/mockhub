package com.mockhub.order.dto;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * What a purchase costs, broken out the way the buyer sees it on the receipt.
 *
 * <p>This exists so that the amount a mandate authorizes and the amount the buyer is
 * actually charged are computed in exactly one place. When they were computed separately,
 * mandates were validated against the subtotal while buyers were charged the total, so a
 * $35.00 ceiling silently authorized a $38.50 purchase.
 */
public record OrderPricing(
        BigDecimal subtotal,
        BigDecimal serviceFee,
        BigDecimal total
) {

    private static final BigDecimal SERVICE_FEE_RATE = new BigDecimal("0.10");

    /** Applies the service fee to a subtotal, rounding the fee to cents. */
    public static OrderPricing forSubtotal(BigDecimal subtotal) {
        if (subtotal == null) {
            throw new IllegalArgumentException("Subtotal is required");
        }
        BigDecimal serviceFee = subtotal.multiply(SERVICE_FEE_RATE).setScale(2, RoundingMode.HALF_UP);
        return new OrderPricing(subtotal, serviceFee, subtotal.add(serviceFee));
    }

    /**
     * The all-in amount to authorize a purchase against — what the buyer will be charged,
     * not the ticket price alone.
     */
    public static BigDecimal totalForSubtotal(BigDecimal subtotal) {
        return forSubtotal(subtotal).total();
    }
}
