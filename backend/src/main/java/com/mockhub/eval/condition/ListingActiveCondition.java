package com.mockhub.eval.condition;

import org.springframework.stereotype.Component;

import com.mockhub.eval.EvalCondition;
import com.mockhub.eval.dto.EvalContext;
import com.mockhub.eval.dto.EvalResult;
import com.mockhub.eval.dto.EvalSeverity;
import com.mockhub.ticket.entity.Listing;

@Component
public class ListingActiveCondition implements EvalCondition {

    @Override
    public String name() {
        return "listing-active";
    }

    @Override
    public boolean appliesTo(EvalContext context) {
        return context.listing() != null;
    }

    @Override
    public EvalResult evaluate(EvalContext context) {
        Listing listing = context.listing();

        if (!"ACTIVE".equals(listing.getStatus())) {
            return EvalResult.fail(name(), EvalSeverity.CRITICAL,
                    "Listing status is not ACTIVE: " + listing.getStatus());
        }

        return EvalResult.pass(name());
    }
}
