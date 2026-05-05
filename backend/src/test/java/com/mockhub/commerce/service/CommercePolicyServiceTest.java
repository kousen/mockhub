package com.mockhub.commerce.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.mockhub.commerce.dto.CommercePolicyDto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

class CommercePolicyServiceTest {

    private final CommercePolicyService commercePolicyService = new CommercePolicyService();

    @Test
    @DisplayName("getDefaultPolicy - returns structured policy sections for all purchases")
    void getDefaultPolicy_returnsStructuredPolicySections() {
        CommercePolicyDto policy = commercePolicyService.getDefaultPolicy();

        assertEquals("mockhub-standard-commerce-policy", policy.policyId());
        assertEquals("all_mockhub_ticket_purchases", policy.appliesTo());
        assertNull(policy.eventSlug());
        assertEquals("/api/v1/commerce/policies/default", policy.policyUrl());
        assertFalse(policy.sections().isEmpty(), "Policy should include structured sections");
        assertEquals("refunds", policy.sections().getFirst().code());
    }

    @Test
    @DisplayName("getPolicyForEvent - given event slug - returns event-scoped policy URL")
    void getPolicyForEvent_givenEventSlug_returnsEventScopedPolicyUrl() {
        CommercePolicyDto policy = commercePolicyService.getPolicyForEvent(" rock-festival ");

        assertEquals("event:rock-festival", policy.appliesTo());
        assertEquals("rock-festival", policy.eventSlug());
        assertEquals("/api/v1/commerce/policies/events/rock-festival", policy.policyUrl());
    }

    @Test
    @DisplayName("getPolicyUrlForEvent - given blank slug - returns default policy URL")
    void getPolicyUrlForEvent_givenBlankSlug_returnsDefaultPolicyUrl() {
        assertEquals("/api/v1/commerce/policies/default",
                commercePolicyService.getPolicyUrlForEvent("   "));
    }
}
