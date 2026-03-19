package com.mockhub.payment.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PaymentServiceSealedTest {

    @Test
    @DisplayName("PaymentService - is NOT sealed (Mockito cannot mock sealed interfaces)")
    void paymentService_isNotSealed() {
        // PaymentService is intentionally left unsealed. Sealed interfaces
        // cannot be mocked by Mockito (which creates dynamic subclasses),
        // breaking @WebMvcTest controller tests that use @MockitoBean.
        // This is a pragmatic trade-off: testability > closed polymorphism.
        assertFalse(PaymentService.class.isSealed(),
                "PaymentService should NOT be sealed — Mockito compatibility");
    }

    @Test
    @DisplayName("MockPaymentService - implements PaymentService")
    void mockPaymentService_implementsPaymentService() {
        assertTrue(PaymentService.class.isAssignableFrom(MockPaymentService.class));
    }

    @Test
    @DisplayName("StripePaymentService - implements PaymentService")
    void stripePaymentService_implementsPaymentService() {
        assertTrue(PaymentService.class.isAssignableFrom(StripePaymentService.class));
    }
}
