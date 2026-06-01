package com.mockhub.order.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderNumberSupportTest {

    @Test
    @DisplayName("normalizeGeneratedOrderNumber - valid number with whitespace - returns stripped value")
    void normalizeGeneratedOrderNumber_givenValidNumberWithWhitespace_returnsStrippedValue() {
        assertThat(OrderNumberSupport.normalizeGeneratedOrderNumber("  MH-20260319-0001  "))
                .isEqualTo("MH-20260319-0001");
    }

    @Test
    @DisplayName("normalizeGeneratedOrderNumber - blank number - throws required error")
    void normalizeGeneratedOrderNumber_givenBlankNumber_throwsRequiredError() {
        assertThatThrownBy(() -> OrderNumberSupport.normalizeGeneratedOrderNumber(" "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Order number is required");
    }

    @Test
    @DisplayName("normalizeGeneratedOrderNumber - null number - throws required error")
    void normalizeGeneratedOrderNumber_givenNullNumber_throwsRequiredError() {
        assertThatThrownBy(() -> OrderNumberSupport.normalizeGeneratedOrderNumber(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Order number is required");
    }

    @Test
    @DisplayName("normalizeGeneratedOrderNumber - malformed number - throws format error")
    void normalizeGeneratedOrderNumber_givenMalformedNumber_throwsFormatError() {
        assertThatThrownBy(() -> OrderNumberSupport.normalizeGeneratedOrderNumber("MH-INVALID"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Order number must use format MH-YYYYMMDD-0001");
    }
}
