package com.mockhub.notification.service;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile("sms-twilio")
@Primary
public class TwilioSmsDeliveryService implements SmsDeliveryService {

    private static final Logger log = LoggerFactory.getLogger(TwilioSmsDeliveryService.class);

    private final String fromNumber;

    public TwilioSmsDeliveryService(
            @Value("${TWILIO_ACCOUNT_SID}") String accountSid,
            @Value("${TWILIO_AUTH_TOKEN}") String authToken,
            @Value("${mockhub.sms.from-number}") String fromNumber) {
        this.fromNumber = fromNumber;
        Twilio.init(accountSid, authToken);
    }

    @Override
    public String sendSms(String toPhoneNumber, String messageText) {
        try {
            Message message =
                    Message.creator(new PhoneNumber(toPhoneNumber), new PhoneNumber(fromNumber), messageText)
                            .create();
            log.info("SMS sent to {} — SID: {}", toPhoneNumber, message.getSid());
            return message.getSid();
        } catch (Exception e) {
            log.error("Failed to send SMS to {}: {}", toPhoneNumber, e.getMessage(), e);
            return null;
        }
    }
}
