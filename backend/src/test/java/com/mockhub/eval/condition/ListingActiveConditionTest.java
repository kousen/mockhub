package com.mockhub.eval.condition;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.mockhub.eval.dto.EvalContext;
import com.mockhub.eval.dto.EvalResult;
import com.mockhub.eval.dto.EvalSeverity;
import com.mockhub.ticket.entity.Listing;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ListingActiveCondition")
class ListingActiveConditionTest {

    private ListingActiveCondition condition;

    @BeforeEach
    void setUp() {
        condition = new ListingActiveCondition();
    }

    @Test
    @DisplayName("name returns listing-active")
    void name_always_returnsListingActive() {
        assertThat(condition.name()).isEqualTo("listing-active");
    }

    @Test
    @DisplayName("appliesTo returns true when listing is present")
    void appliesTo_givenListingPresent_returnsTrue() {
        Listing listing = new Listing();
        EvalContext context = EvalContext.forListing(listing);

        assertThat(condition.appliesTo(context)).isTrue();
    }

    @Test
    @DisplayName("appliesTo returns false when listing is null")
    void appliesTo_givenNoListing_returnsFalse() {
        EvalContext context = EvalContext.forEvent(null);

        assertThat(condition.appliesTo(context)).isFalse();
    }

    @Test
    @DisplayName("evaluate passes for ACTIVE listing")
    void evaluate_givenActiveListing_passes() {
        Listing listing = new Listing();
        listing.setStatus("ACTIVE");
        EvalContext context = EvalContext.forListing(listing);

        EvalResult result = condition.evaluate(context);

        assertThat(result.passed()).isTrue();
        assertThat(result.conditionName()).isEqualTo("listing-active");
    }

    @Test
    @DisplayName("evaluate fails with CRITICAL for SOLD listing")
    void evaluate_givenSoldListing_failsCritical() {
        Listing listing = new Listing();
        listing.setStatus("SOLD");
        EvalContext context = EvalContext.forListing(listing);

        EvalResult result = condition.evaluate(context);

        assertThat(result.passed()).isFalse();
        assertThat(result.severity()).isEqualTo(EvalSeverity.CRITICAL);
        assertThat(result.message()).isEqualTo("Listing status is not ACTIVE: SOLD");
    }

    @Test
    @DisplayName("evaluate fails with CRITICAL for EXPIRED listing")
    void evaluate_givenExpiredListing_failsCritical() {
        Listing listing = new Listing();
        listing.setStatus("EXPIRED");
        EvalContext context = EvalContext.forListing(listing);

        EvalResult result = condition.evaluate(context);

        assertThat(result.passed()).isFalse();
        assertThat(result.severity()).isEqualTo(EvalSeverity.CRITICAL);
        assertThat(result.message()).isEqualTo("Listing status is not ACTIVE: EXPIRED");
    }
}
