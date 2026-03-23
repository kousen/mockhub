package com.mockhub.acp.dto;

import jakarta.validation.constraints.NotNull;

public record AcpLineItem(
        @NotNull(message = "Listing ID is required")
        Long listingId,
        int quantity
) {

    public AcpLineItem {
        if (quantity <= 0) {
            quantity = 1;
        }
    }
}
