package com.mockhub.order.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.mockhub.common.exception.ConflictException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaymentMethodSupportTest {

    @Test
    @DisplayName("normalize - uppercase supported method - returns lowercase value")
    void normalize_givenUppercaseSupportedMethod_returnsLowercaseValue() {
        assertThat(PaymentMethodSupport.normalize(" STRIPE ")).isEqualTo("stripe");
    }

    @Test
    @DisplayName("normalize - blank method - throws required error")
    void normalize_givenBlankMethod_throwsRequiredError() {
        assertThatThrownBy(() -> PaymentMethodSupport.normalize(" "))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Payment method is required");
    }

    @Test
    @DisplayName("normalize - null method - throws required error")
    void normalize_givenNullMethod_throwsRequiredError() {
        assertThatThrownBy(() -> PaymentMethodSupport.normalize(null))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Payment method is required");
    }

    @Test
    @DisplayName("normalize - long unsupported method - abbreviates error value")
    void normalize_givenLongUnsupportedMethod_abbreviatesErrorValue() {
        String unsupported = "x".repeat(100);

        assertThatThrownBy(() -> PaymentMethodSupport.normalize(unsupported))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("x".repeat(77) + "...")
                .hasMessageContaining("Supported payment methods are: mock, stripe");
    }

    @Test
    @DisplayName("normalizeOrMock - null method - defaults to mock")
    void normalizeOrMock_givenNullMethod_defaultsToMock() {
        assertThat(PaymentMethodSupport.normalizeOrMock(null)).isEqualTo("mock");
    }
}
