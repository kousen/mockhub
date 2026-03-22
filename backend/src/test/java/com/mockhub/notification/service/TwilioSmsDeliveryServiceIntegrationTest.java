package com.mockhub.notification.service;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test that sends a real SMS via Twilio.
 * Only runs when TWILIO_ACCOUNT_SID, TWILIO_AUTH_TOKEN, and TWILIO_PHONE_NUMBER
 * environment variables are set. Tagged "twilio" so it can be excluded from
 * normal test runs and invoked explicitly:
 *
 *   ./gradlew test -PincludeTags=twilio
 */
@Tag("twilio")
@EnabledIfEnvironmentVariable(named = "TWILIO_ACCOUNT_SID", matches = "AC.+")
@EnabledIfEnvironmentVariable(named = "TWILIO_AUTH_TOKEN", matches = ".+")
@EnabledIfEnvironmentVariable(named = "TWILIO_PHONE_NUMBER", matches = "\\+.+")
class TwilioSmsDeliveryServiceIntegrationTest {

    @Test
    void sendSms_givenValidCredentials_returnsMessageSid() {
        String accountSid = System.getenv("TWILIO_ACCOUNT_SID");
        String authToken = System.getenv("TWILIO_AUTH_TOKEN");
        String fromNumber = System.getenv("TWILIO_PHONE_NUMBER");
        String toNumber = System.getenv().getOrDefault("TWILIO_TEST_PHONE", "+18608824279");

        TwilioSmsDeliveryService service =
                new TwilioSmsDeliveryService(accountSid, authToken, fromNumber);

        String sid = service.sendSms(toNumber, "MockHub integration test — if you receive this, Twilio is working!");

        assertThat(sid)
                .as("Twilio should return a message SID starting with 'SM'")
                .isNotNull()
                .startsWith("SM");
    }
}
