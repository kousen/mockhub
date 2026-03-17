package com.mockhub;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class EventIntegrationTest extends AbstractIntegrationTest {

    @Test
    @DisplayName("GET /api/v1/events - returns paged events list")
    void listEvents_returnsPagedEventsList() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                "/api/v1/events", String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode(),
                "Events endpoint should return 200");
        assertNotNull(response.getBody(), "Response body should not be null");
    }

    @Test
    @DisplayName("GET /api/v1/events/featured - returns featured events")
    void listFeaturedEvents_returnsFeaturedList() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                "/api/v1/events/featured", String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode(),
                "Featured events endpoint should return 200");
    }

    @Test
    @DisplayName("GET /api/v1/events with search param - returns filtered events")
    void searchEvents_returnsFilteredResults() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                "/api/v1/events?q=concert", String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode(),
                "Search endpoint should return 200");
    }

    @Test
    @DisplayName("GET /api/v1/events/{slug} - given nonexistent slug - returns 404")
    void getEvent_givenNonexistentSlug_returns404() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                "/api/v1/events/nonexistent-slug-12345", String.class);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode(),
                "Nonexistent event slug should return 404");
    }
}
