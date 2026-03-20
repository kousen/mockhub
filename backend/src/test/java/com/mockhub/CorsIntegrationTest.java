package com.mockhub;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class CorsIntegrationTest extends AbstractIntegrationTest {

    private static final String ALLOWED_ORIGIN = "http://localhost:5173";
    private static final String DISALLOWED_ORIGIN = "http://evil.com";

    @Test
    @DisplayName("GET request with allowed origin returns Access-Control-Allow-Origin")
    void request_givenAllowedOrigin_returnsCorsHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Origin", ALLOWED_ORIGIN);

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/events", HttpMethod.GET,
                new HttpEntity<>(headers), String.class);

        assertEquals(ALLOWED_ORIGIN,
                response.getHeaders().getFirst("Access-Control-Allow-Origin"));
    }

    @Test
    @DisplayName("GET request with allowed origin returns Allow-Credentials: true")
    void request_givenAllowedOrigin_returnsAllowCredentials() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Origin", ALLOWED_ORIGIN);

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/events", HttpMethod.GET,
                new HttpEntity<>(headers), String.class);

        assertEquals("true",
                response.getHeaders().getFirst("Access-Control-Allow-Credentials"));
    }

    @Test
    @DisplayName("GET request with disallowed origin returns no CORS headers")
    void request_givenDisallowedOrigin_returnsNoCorsHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Origin", DISALLOWED_ORIGIN);

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/events", HttpMethod.GET,
                new HttpEntity<>(headers), String.class);

        assertNull(response.getHeaders().getFirst("Access-Control-Allow-Origin"));
    }
}
