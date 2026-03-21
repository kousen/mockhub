package com.mockhub.notification.service;

public interface SmsDeliveryService {

    void sendSms(String toPhoneNumber, String message);
}
