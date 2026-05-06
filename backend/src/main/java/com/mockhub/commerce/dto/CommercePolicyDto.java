package com.mockhub.commerce.dto;

import java.time.Instant;
import java.util.List;

public record CommercePolicyDto(
        String policyId,
        String version,
        String appliesTo,
        String eventSlug,
        List<CommercePolicySectionDto> sections,
        String supportContact,
        String supportUrl,
        String policyUrl,
        Instant updatedAt
) {
}
