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
        return context.agentId() != null || context.mandateId() != null;
    }

    @Override
    public EvalResult evaluate(EvalContext context) {
        String agentId = context.agentId();
        String mandateId = context.mandateId();
        String userEmail = context.userEmail();

        if (agentId == null || agentId.isBlank()) {
            return EvalResult.fail(name(), EvalSeverity.CRITICAL,
                    "Agent ID is required for autonomous agent actions");
        }

        if (userEmail == null) {
            return EvalResult.fail(name(), EvalSeverity.CRITICAL,
                    "Cannot determine user email for mandate validation");
        }

        String requiredScope = determineRequiredScope(context);
        if ("PURCHASE".equals(requiredScope) && (mandateId == null || mandateId.isBlank())) {
            return EvalResult.fail(name(), EvalSeverity.CRITICAL,
                    "Mandate ID is required for autonomous purchase actions");
        }

        Mandate mandate = mandateService.getActiveMandate(agentId, userEmail, mandateId);
        if (mandate == null) {
            return EvalResult.fail(name(), EvalSeverity.CRITICAL,
                    "No active mandate for agent '" + agentId + "' and user '" + userEmail
                            + "' matching mandate '" + mandateId + "'");
        }

        if (!mandateService.validateAction(agentId, userEmail, requiredScope,
                context.orderTotal(), context.categorySlug(), resolveEventSlug(context), mandateId)) {
            return EvalResult.fail(name(), EvalSeverity.CRITICAL,
                    "Mandate does not authorize this action (scope=" + requiredScope
                            + ", amount=" + context.orderTotal()
                            + ", mandateId=" + mandateId + ")");
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
