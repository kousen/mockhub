package com.mockhub.notification.service;

public interface EmailDeliveryService {

    /**
     * Sends an email and returns the provider's message identifier,
     * or null if sending failed.
     */
    String sendEmail(String toAddress, String subject, String htmlBody);
}
