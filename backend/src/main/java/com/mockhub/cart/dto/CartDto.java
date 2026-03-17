package com.mockhub.cart.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record CartDto(
        Long id,
        Long userId,
        List<CartItemDto> items,
        BigDecimal subtotal,
        int itemCount,
        Instant expiresAt
) {
}
