package com.mockhub.notification.service;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@Profile("email-resend")
@Primary
public class ResendEmailDeliveryService implements EmailDeliveryService {

    private static final Logger log = LoggerFactory.getLogger(ResendEmailDeliveryService.class);

    private final RestClient restClient;
    private final String fromAddress;

    public ResendEmailDeliveryService(
            @Value("${RESEND_API_KEY}") String apiKey,
            @Value("${mockhub.email.from-address}") String fromAddress) {
        this.fromAddress = fromAddress;
        this.restClient = RestClient.builder()
                .baseUrl("https://api.resend.com")
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .build();
    }

    ResendEmailDeliveryService(RestClient restClient, String fromAddress) {
        this.restClient = restClient;
        this.fromAddress = fromAddress;
    }

    @Override
    public String sendEmail(String toAddress, String subject, String htmlBody) {
        try {
            Map<String, Object> request = Map.of(
                    "from", fromAddress,
                    "to", List.of(toAddress),
                    "subject", subject,
                    "html", htmlBody
            );

            Map<?, ?> response = restClient.post()
                    .uri("/emails")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(Map.class);

            String emailId = response != null ? String.valueOf(response.get("id")) : null;
            log.info("Email sent to {} via Resend — ID: {}", toAddress, emailId);
            return emailId;
        } catch (Exception exception) {
            log.error("Failed to send email to {}: {}", toAddress, exception.getMessage(), exception);
            return null;
        }
    }
}
