package com.mockhub.order.dto;

import java.util.Set;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CheckoutRequestTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    @DisplayName("paymentMethod - uppercase supported method - passes validation")
    void paymentMethod_givenUppercaseSupportedMethod_passesValidation() {
        Set<ConstraintViolation<CheckoutRequest>> violations =
                validator.validate(new CheckoutRequest("STRIPE"));

        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("paymentMethod - supported method with whitespace - passes validation")
    void paymentMethod_givenSupportedMethodWithWhitespace_passesValidation() {
        Set<ConstraintViolation<CheckoutRequest>> violations =
                validator.validate(new CheckoutRequest(" stripe "));

        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("paymentMethod - verbose unsupported method - fails validation")
    void paymentMethod_givenVerboseUnsupportedMethod_failsValidation() {
        Set<ConstraintViolation<CheckoutRequest>> violations =
                validator.validate(new CheckoutRequest("use the mock payment fallback for this purchase"));

        assertThat(violations)
                .extracting(ConstraintViolation::getMessage)
                .contains("Payment method must be one of: mock, stripe");
    }

    @Test
    @DisplayName("paymentMethod - blank method - fails required validation")
    void paymentMethod_givenBlankMethod_failsRequiredValidation() {
        Set<ConstraintViolation<CheckoutRequest>> violations =
                validator.validate(new CheckoutRequest(" "));

        assertThat(violations)
                .extracting(ConstraintViolation::getMessage)
                .contains("Payment method is required");
    }

    @Test
    @DisplayName("paymentMethod - overlong method - fails size validation")
    void paymentMethod_givenOverlongMethod_failsSizeValidation() {
        Set<ConstraintViolation<CheckoutRequest>> violations =
                validator.validate(new CheckoutRequest("x".repeat(31)));

        assertThat(violations)
                .extracting(ConstraintViolation::getMessage)
                .contains("Payment method must not exceed 30 characters");
    }
}
