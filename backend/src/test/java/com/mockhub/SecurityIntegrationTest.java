package com.mockhub;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class SecurityIntegrationTest extends AbstractIntegrationTest {

    @Test
    @DisplayName("GET /api/v1/venues returns 200 without authentication")
    void getVenues_givenNoAuth_returns200() {
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/venues", HttpMethod.GET, null, String.class);

        assertEquals(200, response.getStatusCode().value());
    }

    @Test
    @DisplayName("GET /api/v1/categories returns 200 without authentication")
    void getCategories_givenNoAuth_returns200() {
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/categories", HttpMethod.GET, null, String.class);

        assertEquals(200, response.getStatusCode().value());
    }

    @Test
    @DisplayName("GET /api/v1/tags returns 200 without authentication")
    void getTags_givenNoAuth_returns200() {
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/tags", HttpMethod.GET, null, String.class);

        assertEquals(200, response.getStatusCode().value());
    }

    @Test
    @DisplayName("GET /api/v1/search?q=test returns 200 without authentication")
    void search_givenNoAuth_returns200() {
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/search?q=test", HttpMethod.GET, null, String.class);

        assertEquals(200, response.getStatusCode().value());
    }

    @Test
    @DisplayName("GET /api/v1/orders returns 401 without authentication")
    void getOrders_givenNoAuth_returns401() {
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/orders", HttpMethod.GET, null, String.class);

        assertEquals(401, response.getStatusCode().value());
    }

    @Test
    @DisplayName("GET /api/v1/favorites returns 401 without authentication")
    void getFavorites_givenNoAuth_returns401() {
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/favorites", HttpMethod.GET, null, String.class);

        assertEquals(401, response.getStatusCode().value());
    }

    @Test
    @DisplayName("Protected endpoint returns plain 401 without WWW-Authenticate or Location headers")
    void protectedEndpoint_givenNoAuth_returnsPlain401() {
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/orders", HttpMethod.GET, null, String.class);

        assertEquals(401, response.getStatusCode().value());
        assertNull(response.getHeaders().getFirst("WWW-Authenticate"));
        assertNull(response.getHeaders().getFirst("Location"));
    }

    @Test
    @DisplayName("Protected endpoint returns 401 with an expired/invalid token")
    void protectedEndpoint_givenExpiredToken_returns401() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth("expired.jwt.token");

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/orders", HttpMethod.GET, new HttpEntity<>(headers), String.class);

        assertEquals(401, response.getStatusCode().value());
    }
}
