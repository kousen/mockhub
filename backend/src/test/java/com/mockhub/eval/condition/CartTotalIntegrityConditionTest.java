package com.mockhub.eval.condition;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.mockhub.cart.dto.CartDto;
import com.mockhub.cart.dto.CartItemDto;
import com.mockhub.eval.dto.EvalContext;
import com.mockhub.eval.dto.EvalResult;
import com.mockhub.eval.dto.EvalSeverity;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CartTotalIntegrityCondition")
class CartTotalIntegrityConditionTest {

    private CartTotalIntegrityCondition condition;

    @BeforeEach
    void setUp() {
        condition = new CartTotalIntegrityCondition();
    }

    @Test
    @DisplayName("name returns cart-total-integrity")
    void name_always_returnsCartTotalIntegrity() {
        assertThat(condition.name()).isEqualTo("cart-total-integrity");
    }

    @Test
    @DisplayName("appliesTo returns true when cart is present")
    void appliesTo_givenCartPresent_returnsTrue() {
        CartDto cart = new CartDto(1L, 1L, List.of(), BigDecimal.ZERO, 0,
                Instant.now().plus(30, ChronoUnit.MINUTES));
        EvalContext context = EvalContext.forCart(cart);

        assertThat(condition.appliesTo(context)).isTrue();
    }

    @Test
    @DisplayName("appliesTo returns false when cart is null")
    void appliesTo_givenNoCart_returnsFalse() {
        EvalContext context = EvalContext.forCart(null);

        assertThat(condition.appliesTo(context)).isFalse();
    }

    @Test
    @DisplayName("evaluate passes when cart subtotal matches sum of item prices")
    void evaluate_givenMatchingSubtotal_passes() {
        CartItemDto item1 = createCartItem(1L, new BigDecimal("50.00"));
        CartItemDto item2 = createCartItem(2L, new BigDecimal("75.00"));
        CartDto cart = new CartDto(1L, 1L, List.of(item1, item2),
                new BigDecimal("125.00"), 2,
                Instant.now().plus(30, ChronoUnit.MINUTES));

        EvalContext context = EvalContext.forCart(cart);

        EvalResult result = condition.evaluate(context);

        assertThat(result.passed()).isTrue();
        assertThat(result.conditionName()).isEqualTo("cart-total-integrity");
    }

    @Test
    @DisplayName("evaluate fails with CRITICAL when subtotal does not match")
    void evaluate_givenMismatchedSubtotal_failsCritical() {
        CartItemDto item1 = createCartItem(1L, new BigDecimal("50.00"));
        CartItemDto item2 = createCartItem(2L, new BigDecimal("75.00"));
        CartDto cart = new CartDto(1L, 1L, List.of(item1, item2),
                new BigDecimal("200.00"), 2,
                Instant.now().plus(30, ChronoUnit.MINUTES));

        EvalContext context = EvalContext.forCart(cart);

        EvalResult result = condition.evaluate(context);

        assertThat(result.passed()).isFalse();
        assertThat(result.severity()).isEqualTo(EvalSeverity.CRITICAL);
        assertThat(result.message()).contains("125.00");
        assertThat(result.message()).contains("200.00");
    }

    @Test
    @DisplayName("evaluate passes for empty cart with zero subtotal")
    void evaluate_givenEmptyCartWithZeroSubtotal_passes() {
        CartDto cart = new CartDto(1L, 1L, List.of(),
                BigDecimal.ZERO, 0,
                Instant.now().plus(30, ChronoUnit.MINUTES));

        EvalContext context = EvalContext.forCart(cart);

        EvalResult result = condition.evaluate(context);

        assertThat(result.passed()).isTrue();
    }

    @Test
    @DisplayName("evaluate passes when rounding difference is within tolerance")
    void evaluate_givenSmallRoundingDifference_passes() {
        CartItemDto item1 = createCartItem(1L, new BigDecimal("33.33"));
        CartItemDto item2 = createCartItem(2L, new BigDecimal("33.33"));
        CartItemDto item3 = createCartItem(3L, new BigDecimal("33.33"));
        CartDto cart = new CartDto(1L, 1L, List.of(item1, item2, item3),
                new BigDecimal("99.99"), 3,
                Instant.now().plus(30, ChronoUnit.MINUTES));

        EvalContext context = EvalContext.forCart(cart);

        EvalResult result = condition.evaluate(context);

        assertThat(result.passed()).isTrue();
    }

    private CartItemDto createCartItem(Long id, BigDecimal currentPrice) {
        return new CartItemDto(
                id,
                id,
                "Event " + id,
                "event-" + id,
                "Section A",
                "Row 1",
                "Seat " + id,
                "STANDARD",
                currentPrice,
                currentPrice,
                Instant.now()
        );
    }
}
