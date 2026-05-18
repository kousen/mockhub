package com.mockhub.buyerpreference.dto;

import com.mockhub.ticket.dto.ListingSearchCriteria;

public record BuyerPreferenceApplication(
        ListingSearchCriteria criteria,
        String preferredSection,
        BuyerPreferenceContextDto context
) {
}
