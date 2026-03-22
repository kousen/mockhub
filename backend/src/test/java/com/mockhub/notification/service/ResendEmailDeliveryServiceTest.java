package com.mockhub.notification.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class ResendEmailDeliveryServiceTest {

    private ResendEmailDeliveryService service;
    private MockRestServiceServer mockServer;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder()
                .baseUrl("https://api.resend.com");

        mockServer = MockRestServiceServer.bindTo(builder).build();
        service = new ResendEmailDeliveryService(builder.build(), "tickets@updates.kousenit.com");
    }

    @Test
    void sendEmail_givenValidInput_returnsEmailId() {
        mockServer.expect(requestTo("https://api.resend.com/emails"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("{\"id\": \"email_123abc\"}", MediaType.APPLICATION_JSON));

        String id = service.sendEmail("buyer@example.com", "Order Confirmed", "<h1>Hi</h1>");

        assertThat(id).isEqualTo("email_123abc");
        mockServer.verify();
    }

    @Test
    void sendEmail_givenServerError_returnsNull() {
        mockServer.expect(requestTo("https://api.resend.com/emails"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withServerError());

        String id = service.sendEmail("buyer@example.com", "Order Confirmed", "<h1>Hi</h1>");

        assertThat(id).isNull();
        mockServer.verify();
    }
}
