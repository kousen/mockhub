package com.mockhub.eval.condition;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;

import com.mockhub.eval.EvalCondition;
import com.mockhub.eval.dto.EvalContext;
import com.mockhub.eval.dto.EvalResult;
import com.mockhub.eval.dto.EvalSeverity;
import com.mockhub.mandate.entity.Mandate;
import com.mockhub.mandate.service.MandateService;

@Component
public class MandateCondition implements EvalCondition {

    private final MandateService mandateService;

    public MandateCondition(MandateService mandateService) {
        this.mandateService = mandateService;
    }

    @Override
    public String name() {
        return "mandate-authorization";
    }

    @Override
    public boolean appliesTo(EvalContext context) {
        return context.agentId() != null;
    }

    @Override
    public EvalResult evaluate(EvalContext context) {
        String agentId = context.agentId();
        String userEmail = context.userEmail();

        if (userEmail == null) {
            return EvalResult.fail(name(), EvalSeverity.CRITICAL,
                    "Cannot determine user email for mandate validation");
        }

        Mandate mandate = mandateService.getActiveMandate(agentId, userEmail);
        if (mandate == null) {
            return EvalResult.fail(name(), EvalSeverity.CRITICAL,
                    "No active mandate for agent '" + agentId + "' and user '" + userEmail + "'");
        }

        String requiredScope = determineRequiredScope(context);
        if (!mandateService.validateAction(agentId, userEmail, requiredScope,
                context.orderTotal(), context.categorySlug(), resolveEventSlug(context))) {
            return EvalResult.fail(name(), EvalSeverity.CRITICAL,
                    "Mandate does not authorize this action (scope=" + requiredScope
                            + ", amount=" + context.orderTotal() + ")");
        }

        return EvalResult.pass(name());
    }

    private String determineRequiredScope(EvalContext context) {
        if (context.orderTotal() != null && context.orderTotal().compareTo(BigDecimal.ZERO) > 0) {
            return "PURCHASE";
        }
        return "BROWSE";
    }

    private String resolveEventSlug(EvalContext context) {
        if (context.event() != null) {
            return context.event().getSlug();
        }
        if (context.listing() != null && context.listing().getEvent() != null) {
            return context.listing().getEvent().getSlug();
        }
        return null;
    }
}
