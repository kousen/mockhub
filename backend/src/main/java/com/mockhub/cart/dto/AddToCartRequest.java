package com.mockhub.cart.dto;

import jakarta.validation.constraints.NotNull;

public record AddToCartRequest(
        @NotNull(message = "Listing ID is required")
        Long listingId
) {
}
