package com.mockhub.eval.condition;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.mockhub.eval.EvalCondition;
import com.mockhub.eval.dto.EvalContext;
import com.mockhub.eval.dto.EvalResult;
import com.mockhub.eval.dto.EvalSeverity;

@Component
public class PricePlausibilityCondition implements EvalCondition {

    private final double minRatio;
    private final double maxRatio;

    public PricePlausibilityCondition(
            @Value("${mockhub.eval.price-plausibility.min-ratio:0.1}") double minRatio,
            @Value("${mockhub.eval.price-plausibility.max-ratio:10.0}") double maxRatio) {
        this.minRatio = minRatio;
        this.maxRatio = maxRatio;
    }

    @Override
    public String name() {
        return "price-plausibility";
    }

    @Override
    public boolean appliesTo(EvalContext context) {
        return context.predictedPrice() != null && context.currentPrice() != null;
    }

    @Override
    public EvalResult evaluate(EvalContext context) {
        BigDecimal predictedPrice = context.predictedPrice();
        BigDecimal currentPrice = context.currentPrice();

        if (currentPrice.compareTo(BigDecimal.ZERO) == 0) {
            return EvalResult.skip(name(), "Current price is zero, cannot compute ratio");
        }

        double ratio = predictedPrice.divide(currentPrice, 10, RoundingMode.HALF_UP).doubleValue();

        if (ratio < minRatio || ratio > maxRatio) {
            return EvalResult.fail(name(), EvalSeverity.WARNING,
                    "Predicted price " + predictedPrice + " is implausible relative to current price "
                            + currentPrice + " (ratio: " + String.format("%.4f", ratio)
                            + ", allowed: " + minRatio + "-" + maxRatio + ")");
        }

        return EvalResult.pass(name());
    }
}
