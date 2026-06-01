package com.mockhub.order.service;

import java.util.List;
import java.util.Locale;
import java.util.Set;

import com.mockhub.common.exception.ConflictException;

public final class PaymentMethodSupport {

    private static final String MOCK = "mock";
    private static final List<String> SUPPORTED_METHODS = List.of(MOCK, "stripe");
    private static final Set<String> SUPPORTED_METHOD_SET = Set.copyOf(SUPPORTED_METHODS);
    private static final int DISPLAY_LIMIT = 80;

    private PaymentMethodSupport() {
    }

    public static String normalize(String paymentMethod) {
        if (paymentMethod == null || paymentMethod.isBlank()) {
            throw new ConflictException("Payment method is required");
        }

        String normalized = paymentMethod.strip().toLowerCase(Locale.ROOT);
        if (!SUPPORTED_METHOD_SET.contains(normalized)) {
            throw new ConflictException("Unsupported payment method '" + abbreviate(paymentMethod.strip())
                    + "'. Supported payment methods are: " + supportedValues());
        }
        return normalized;
    }

    public static String normalizeOrMock(String paymentMethod) {
        if (paymentMethod == null) {
            return MOCK;
        }
        return normalize(paymentMethod);
    }

    public static String supportedValues() {
        return String.join(", ", SUPPORTED_METHODS);
    }

    private static String abbreviate(String value) {
        if (value.length() <= DISPLAY_LIMIT) {
            return value;
        }
        return value.substring(0, DISPLAY_LIMIT - 3) + "...";
    }
}
