package com.mockhub.eval.condition;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;

import com.mockhub.cart.dto.CartDto;
import com.mockhub.cart.dto.CartItemDto;
import com.mockhub.eval.EvalCondition;
import com.mockhub.eval.dto.EvalContext;
import com.mockhub.eval.dto.EvalResult;
import com.mockhub.eval.dto.EvalSeverity;

@Component
public class CartTotalIntegrityCondition implements EvalCondition {

    private static final BigDecimal TOLERANCE = new BigDecimal("0.01");

    @Override
    public String name() {
        return "cart-total-integrity";
    }

    @Override
    public boolean appliesTo(EvalContext context) {
        return context.cart() != null;
    }

    @Override
    public EvalResult evaluate(EvalContext context) {
        CartDto cart = context.cart();

        BigDecimal computedTotal = BigDecimal.ZERO;
        for (CartItemDto item : cart.items()) {
            computedTotal = computedTotal.add(item.currentPrice());
        }

        BigDecimal difference = computedTotal.subtract(cart.subtotal()).abs();

        if (difference.compareTo(TOLERANCE) > 0) {
            return EvalResult.fail(name(), EvalSeverity.CRITICAL,
                    "Cart subtotal mismatch: expected " + computedTotal
                            + " but found " + cart.subtotal());
        }

        return EvalResult.pass(name());
    }
}
