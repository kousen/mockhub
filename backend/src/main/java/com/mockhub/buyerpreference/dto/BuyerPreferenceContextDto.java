package com.mockhub.buyerpreference.dto;

import java.util.List;

public record BuyerPreferenceContextDto(
        boolean preferencesAvailable,
        boolean applied,
        List<String> appliedFilters,
        List<String> rationale
) {

    public static BuyerPreferenceContextDto none() {
        return new BuyerPreferenceContextDto(false, false, List.of(), List.of());
    }
}
