package com.mockhub.notification.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class MockEmailDeliveryServiceTest {

    private final MockEmailDeliveryService service = new MockEmailDeliveryService();

    @Test
    void sendEmail_givenValidInput_returnsMockId() {
        String id = service.sendEmail(
                "buyer@example.com",
                "Your tickets for Concert",
                "<h1>Order confirmed</h1>");

        assertThat(id).startsWith("MOCK-EMAIL-");
    }

    @Test
    void sendEmail_givenNullAddress_doesNotThrow() {
        assertDoesNotThrow(() -> service.sendEmail(null, "Subject", "<p>Body</p>"));
    }
}
