package com.mockhub.eval.condition;

import java.math.BigDecimal;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.mockhub.eval.EvalCondition;
import com.mockhub.eval.dto.EvalContext;
import com.mockhub.eval.dto.EvalResult;
import com.mockhub.eval.dto.EvalSeverity;

@Component
public class SpendingLimitCondition implements EvalCondition {

    private final BigDecimal maxCartTotal;

    public SpendingLimitCondition(
            @Value("${mockhub.eval.max-cart-total:2000}") BigDecimal maxCartTotal) {
        this.maxCartTotal = maxCartTotal;
    }

    @Override
    public String name() {
        return "spending-limit";
    }

    @Override
    public boolean appliesTo(EvalContext context) {
        return context.cart() != null;
    }

    @Override
    public EvalResult evaluate(EvalContext context) {
        BigDecimal subtotal = context.cart().subtotal();

        if (subtotal.compareTo(maxCartTotal) > 0) {
            return EvalResult.fail(name(), EvalSeverity.WARNING,
                    "Cart total $" + subtotal + " exceeds recommended limit of $" + maxCartTotal);
        }

        return EvalResult.pass(name());
    }
}
