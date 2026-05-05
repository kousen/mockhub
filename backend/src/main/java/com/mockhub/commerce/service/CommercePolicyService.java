package com.mockhub.commerce.service;

import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;

import com.mockhub.commerce.dto.CommercePolicyDto;
import com.mockhub.commerce.dto.CommercePolicySectionDto;

@Service
public class CommercePolicyService {

    private static final String POLICY_ID = "mockhub-standard-commerce-policy";
    private static final String VERSION = "2026-05-01";
    private static final Instant UPDATED_AT = Instant.parse("2026-05-01T00:00:00Z");
    private static final String DEFAULT_POLICY_URL = "/api/v1/commerce/policies/default";

    public CommercePolicyDto getDefaultPolicy() {
        return buildPolicy(null);
    }

    public CommercePolicyDto getPolicyForEvent(String eventSlug) {
        if (eventSlug == null || eventSlug.isBlank()) {
            return getDefaultPolicy();
        }
        return buildPolicy(eventSlug.strip());
    }

    public String getPolicyUrlForEvent(String eventSlug) {
        if (eventSlug == null || eventSlug.isBlank()) {
            return DEFAULT_POLICY_URL;
        }
        return "/api/v1/commerce/policies/events/" + eventSlug.strip();
    }

    private CommercePolicyDto buildPolicy(String eventSlug) {
        return new CommercePolicyDto(
                POLICY_ID,
                VERSION,
                eventSlug == null ? "all_mockhub_ticket_purchases" : "event:" + eventSlug,
                eventSlug,
                policySections(),
                "support@mockhub.dev",
                "/support",
                eventSlug == null ? DEFAULT_POLICY_URL : getPolicyUrlForEvent(eventSlug),
                UPDATED_AT
        );
    }

    private List<CommercePolicySectionDto> policySections() {
        return List.of(
                new CommercePolicySectionDto(
                        "refunds",
                        "Refund policy",
                        "Orders are final after confirmation except when MockHub cancels or cannot fulfill tickets.",
                        "Pending orders can be cancelled before confirmation. Confirmed orders are generally final. "
                                + "If MockHub cancels an event, cannot deliver tickets, or identifies invalid tickets, "
                                + "the buyer is eligible for a refund or comparable replacement offer.",
                        "Before checkout, tell the buyer that confirmed tickets are usually non-refundable unless "
                                + "MockHub cannot fulfill the order."
                ),
                new CommercePolicySectionDto(
                        "cancellations",
                        "Cancellation policy",
                        "Agents may cancel pending checkouts before completion; completed purchases cannot be cancelled.",
                        "ACP checkouts and cart-based orders remain cancellable while they are PENDING. Once payment is "
                                + "confirmed and tickets are issued, cancellation is not available through the checkout API.",
                        "If the buyer asks to change seats or abandon a purchase, cancel before confirming payment."
                ),
                new CommercePolicySectionDto(
                        "ticket_transfer",
                        "Ticket transfer and delivery",
                        "Confirmed tickets are delivered immediately through MockHub's public ticket view.",
                        "Confirmed orders receive token-protected public ticket links with QR codes. Tickets are not "
                                + "mailed, and transfer outside MockHub is not modeled in this teaching system.",
                        "After purchase, use order/ticket links from MockHub instead of promising external transfer."
                ),
                new CommercePolicySectionDto(
                        "fees",
                        "Fee disclosure",
                        "Checkout totals include ticket subtotal, service fee, and all-in total before confirmation.",
                        "Agents must present the all-in total returned by checkout before confirming payment. Listing "
                                + "prices alone are not the final buyer cost because service fees may apply.",
                        "Use checkout pricing fields as the source of truth for final buyer cost."
                ),
                new CommercePolicySectionDto(
                        "support_and_disputes",
                        "Support and recourse",
                        "Support is available through MockHub support when ticket delivery or validation fails.",
                        "For invalid, missing, duplicate-scanned, or otherwise unusable tickets, the buyer should contact "
                                + "MockHub support with the order number and ticket details.",
                        "If fulfillment looks uncertain or a ticket verification problem appears, direct the buyer to support."
                )
        );
    }
}
