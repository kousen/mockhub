package com.mockhub.notification.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile("mock-email")
public class MockEmailDeliveryService implements EmailDeliveryService {

    private static final Logger log = LoggerFactory.getLogger(MockEmailDeliveryService.class);

    @Override
    public String sendEmail(String toAddress, String subject, String htmlBody) {
        log.info("[MOCK EMAIL] To: {} | Subject: {} | Body length: {} chars",
                toAddress, subject, htmlBody.length());
        return "MOCK-EMAIL-" + System.currentTimeMillis();
    }
}
