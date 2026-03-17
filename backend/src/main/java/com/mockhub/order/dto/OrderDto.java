package com.mockhub.order.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderDto(
        Long id,
        String orderNumber,
        String status,
        BigDecimal subtotal,
        BigDecimal serviceFee,
        BigDecimal total,
        String paymentMethod,
        Instant confirmedAt,
        Instant createdAt,
        List<OrderItemDto> items
) {
}
