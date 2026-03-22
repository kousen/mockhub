package com.mockhub.notification.service;

public interface SmsDeliveryService {

    /**
     * Sends an SMS message and returns the provider's message identifier,
     * or null if sending failed.
     */
    String sendSms(String toPhoneNumber, String message);
}
