package com.mockhub.order.service;

import java.util.regex.Pattern;

public final class OrderNumberSupport {

    private static final Pattern GENERATED_ORDER_NUMBER = Pattern.compile("MH-\\d{8}-\\d{4,8}");
    private static final String FORMAT_MESSAGE = "Order number must use format MH-YYYYMMDD-0001";

    private OrderNumberSupport() {
    }

    public static String normalizeGeneratedOrderNumber(String orderNumber) {
        if (orderNumber == null || orderNumber.isBlank()) {
            throw new IllegalArgumentException("Order number is required");
        }
        String normalized = orderNumber.strip();
        if (!GENERATED_ORDER_NUMBER.matcher(normalized).matches()) {
            throw new IllegalArgumentException(FORMAT_MESSAGE);
        }
        return normalized;
    }
}
