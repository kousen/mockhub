package com.mockhub.notification.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile("mock-sms")
public class MockSmsDeliveryService implements SmsDeliveryService {

    private static final Logger log = LoggerFactory.getLogger(MockSmsDeliveryService.class);

    @Override
    public void sendSms(String toPhoneNumber, String message) {
        log.info("[MOCK SMS] To: {} | Message: {}", toPhoneNumber, message);
    }
}
