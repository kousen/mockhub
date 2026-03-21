package com.mockhub.eval.condition;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.mockhub.eval.dto.EvalContext;
import com.mockhub.eval.dto.EvalResult;
import com.mockhub.eval.dto.EvalSeverity;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PricePlausibilityCondition")
class PricePlausibilityConditionTest {

    private PricePlausibilityCondition condition;

    @BeforeEach
    void setUp() {
        condition = new PricePlausibilityCondition(0.1, 10.0);
    }

    @Test
    @DisplayName("name returns price-plausibility")
    void name_always_returnsPricePlausibility() {
        assertThat(condition.name()).isEqualTo("price-plausibility");
    }

    @Test
    @DisplayName("appliesTo returns true when both prices present")
    void appliesTo_givenBothPricesPresent_returnsTrue() {
        EvalContext context = EvalContext.forPricePrediction(
                new BigDecimal("100.00"), new BigDecimal("90.00"));

        assertThat(condition.appliesTo(context)).isTrue();
    }

    @Test
    @DisplayName("appliesTo returns false when predicted price is null")
    void appliesTo_givenNoPredictedPrice_returnsFalse() {
        EvalContext context = EvalContext.forPricePrediction(null, new BigDecimal("90.00"));

        assertThat(condition.appliesTo(context)).isFalse();
    }

    @Test
    @DisplayName("appliesTo returns false when current price is null")
    void appliesTo_givenNoCurrentPrice_returnsFalse() {
        EvalContext context = EvalContext.forPricePrediction(new BigDecimal("100.00"), null);

        assertThat(condition.appliesTo(context)).isFalse();
    }

    @Test
    @DisplayName("evaluate passes for normal price ratio")
    void evaluate_givenNormalPriceRatio_passes() {
        EvalContext context = EvalContext.forPricePrediction(
                new BigDecimal("110.00"), new BigDecimal("100.00"));

        EvalResult result = condition.evaluate(context);

        assertThat(result.passed()).isTrue();
        assertThat(result.conditionName()).isEqualTo("price-plausibility");
    }

    @Test
    @DisplayName("evaluate fails with WARNING when price is too low")
    void evaluate_givenPriceTooLow_failsWarning() {
        EvalContext context = EvalContext.forPricePrediction(
                new BigDecimal("5.00"), new BigDecimal("100.00"));

        EvalResult result = condition.evaluate(context);

        assertThat(result.passed()).isFalse();
        assertThat(result.severity()).isEqualTo(EvalSeverity.WARNING);
        assertThat(result.message()).contains("5.00");
        assertThat(result.message()).contains("100.00");
        assertThat(result.message()).contains("implausible");
    }

    @Test
    @DisplayName("evaluate fails with WARNING when price is too high")
    void evaluate_givenPriceTooHigh_failsWarning() {
        EvalContext context = EvalContext.forPricePrediction(
                new BigDecimal("1500.00"), new BigDecimal("100.00"));

        EvalResult result = condition.evaluate(context);

        assertThat(result.passed()).isFalse();
        assertThat(result.severity()).isEqualTo(EvalSeverity.WARNING);
        assertThat(result.message()).contains("1500.00");
        assertThat(result.message()).contains("100.00");
        assertThat(result.message()).contains("implausible");
    }

    @Test
    @DisplayName("evaluate passes at exact minimum boundary")
    void evaluate_givenExactMinBoundary_passes() {
        EvalContext context = EvalContext.forPricePrediction(
                new BigDecimal("10.00"), new BigDecimal("100.00"));

        EvalResult result = condition.evaluate(context);

        assertThat(result.passed()).isTrue();
    }

    @Test
    @DisplayName("evaluate passes at exact maximum boundary")
    void evaluate_givenExactMaxBoundary_passes() {
        EvalContext context = EvalContext.forPricePrediction(
                new BigDecimal("1000.00"), new BigDecimal("100.00"));

        EvalResult result = condition.evaluate(context);

        assertThat(result.passed()).isTrue();
    }

    @Test
    @DisplayName("evaluate skips when current price is zero")
    void evaluate_givenZeroCurrentPrice_skips() {
        EvalContext context = EvalContext.forPricePrediction(
                new BigDecimal("100.00"), BigDecimal.ZERO);

        EvalResult result = condition.evaluate(context);

        assertThat(result.passed()).isTrue();
        assertThat(result.message()).contains("Skipped");
        assertThat(result.message()).contains("zero");
    }
}
