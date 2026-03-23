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

@DisplayName("SpendingLimitCondition")
class SpendingLimitConditionTest {

    private SpendingLimitCondition condition;

    @BeforeEach
    void setUp() {
        condition = new SpendingLimitCondition(new BigDecimal("500"));
    }

    @Test
    @DisplayName("name returns spending-limit")
    void name_always_returnsSpendingLimit() {
        assertThat(condition.name()).isEqualTo("spending-limit");
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
    @DisplayName("evaluate passes when cart total is under limit")
    void evaluate_givenCartUnderLimit_passes() {
        CartItemDto item = createCartItem(1L, new BigDecimal("100.00"));
        CartDto cart = new CartDto(1L, 1L, List.of(item),
                new BigDecimal("100.00"), 1,
                Instant.now().plus(30, ChronoUnit.MINUTES));

        EvalContext context = EvalContext.forCart(cart);

        EvalResult result = condition.evaluate(context);

        assertThat(result.passed()).isTrue();
        assertThat(result.conditionName()).isEqualTo("spending-limit");
    }

    @Test
    @DisplayName("evaluate passes when cart total equals limit exactly")
    void evaluate_givenCartAtLimit_passes() {
        CartItemDto item = createCartItem(1L, new BigDecimal("500.00"));
        CartDto cart = new CartDto(1L, 1L, List.of(item),
                new BigDecimal("500.00"), 1,
                Instant.now().plus(30, ChronoUnit.MINUTES));

        EvalContext context = EvalContext.forCart(cart);

        EvalResult result = condition.evaluate(context);

        assertThat(result.passed()).isTrue();
    }

    @Test
    @DisplayName("evaluate returns WARNING when cart total exceeds limit")
    void evaluate_givenCartOverLimit_returnsWarning() {
        CartItemDto item1 = createCartItem(1L, new BigDecimal("300.00"));
        CartItemDto item2 = createCartItem(2L, new BigDecimal("300.00"));
        CartDto cart = new CartDto(1L, 1L, List.of(item1, item2),
                new BigDecimal("600.00"), 2,
                Instant.now().plus(30, ChronoUnit.MINUTES));

        EvalContext context = EvalContext.forCart(cart);

        EvalResult result = condition.evaluate(context);

        assertThat(result.passed()).isFalse();
        assertThat(result.severity()).isEqualTo(EvalSeverity.WARNING);
        assertThat(result.message()).contains("600.00");
        assertThat(result.message()).contains("500");
    }

    @Test
    @DisplayName("evaluate uses custom limit from configuration")
    void evaluate_givenCustomLimit_usesConfiguredValue() {
        SpendingLimitCondition customCondition = new SpendingLimitCondition(new BigDecimal("200"));

        CartItemDto item = createCartItem(1L, new BigDecimal("250.00"));
        CartDto cart = new CartDto(1L, 1L, List.of(item),
                new BigDecimal("250.00"), 1,
                Instant.now().plus(30, ChronoUnit.MINUTES));

        EvalContext context = EvalContext.forCart(cart);

        EvalResult result = customCondition.evaluate(context);

        assertThat(result.passed()).isFalse();
        assertThat(result.severity()).isEqualTo(EvalSeverity.WARNING);
        assertThat(result.message()).contains("200");
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
