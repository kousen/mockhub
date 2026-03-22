package com.mockhub.notification.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class MockSmsDeliveryServiceTest {

    private final MockSmsDeliveryService service = new MockSmsDeliveryService();

    @Test
    void sendSms_givenValidInput_returnsMockSid() {
        String sid = service.sendSms("+15551234567", "Your order #123 is confirmed!");

        assertThat(sid).startsWith("MOCK-SID-");
    }

    @Test
    void sendSms_givenNullPhone_doesNotThrow() {
        assertDoesNotThrow(() -> service.sendSms(null, "Test message"));
    }
}
