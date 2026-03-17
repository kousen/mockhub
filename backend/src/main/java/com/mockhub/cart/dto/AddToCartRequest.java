package com.mockhub.cart.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Request to add a ticket listing to the cart")
public record AddToCartRequest(
        @NotNull(message = "Listing ID is required")
        @Schema(description = "ID of the ticket listing to add", example = "42")
        Long listingId
) {
}
