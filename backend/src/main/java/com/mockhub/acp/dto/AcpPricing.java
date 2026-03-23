package com.mockhub.acp.dto;

import java.math.BigDecimal;

public record AcpPricing(
        BigDecimal subtotal,
        BigDecimal serviceFee,
        BigDecimal total,
        String currency
) {
}
