package com.mockhub.event.dto;

import java.math.BigDecimal;
import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EventSearchRequestTest {

    @Test
    @DisplayName("EventSearchRequest - is a record")
    void eventSearchRequest_isARecord() {
        assertTrue(EventSearchRequest.class.isRecord(),
                "EventSearchRequest should be a record");
    }

    @Test
    @DisplayName("EventSearchRequest - applies defaults when all nulls passed")
    void eventSearchRequest_appliesDefaultsWhenAllNullsPassed() {
        EventSearchRequest request = new EventSearchRequest(
                null, null, null, null, null, null, null, null,
                null, null, null, null
        );

        assertEquals("ACTIVE", request.status(),
                "Default status should be ACTIVE");
        assertEquals("eventDate", request.sort(),
                "Default sort should be eventDate");
        assertEquals(0, request.page(),
                "Default page should be 0");
        assertEquals(20, request.size(),
                "Default size should be 20");
    }

    @Test
    @DisplayName("EventSearchRequest - preserves explicit values")
    void eventSearchRequest_preservesExplicitValues() {
        Instant from = Instant.parse("2026-06-01T00:00:00Z");
        Instant to = Instant.parse("2026-12-31T23:59:59Z");

        EventSearchRequest request = new EventSearchRequest(
                "rock", "concerts", "outdoor,festival", "Boston",
                from, to,
                new BigDecimal("25.00"), new BigDecimal("200.00"),
                "ACTIVE", "price", 2, 10
        );

        assertEquals("rock", request.q());
        assertEquals("concerts", request.category());
        assertEquals("outdoor,festival", request.tags());
        assertEquals("Boston", request.city());
        assertEquals(from, request.dateFrom());
        assertEquals(to, request.dateTo());
        assertEquals(new BigDecimal("25.00"), request.minPrice());
        assertEquals(new BigDecimal("200.00"), request.maxPrice());
        assertEquals("ACTIVE", request.status());
        assertEquals("price", request.sort());
        assertEquals(2, request.page());
        assertEquals(10, request.size());
    }

    @Test
    @DisplayName("EventSearchRequest - clamps negative page to zero")
    void eventSearchRequest_clampsNegativePageToZero() {
        EventSearchRequest request = new EventSearchRequest(
                null, null, null, null, null, null, null, null,
                null, null, -5, 20
        );

        assertEquals(0, request.page(),
                "Negative page should be clamped to 0");
    }

    @Test
    @DisplayName("EventSearchRequest - clamps oversized size to 100")
    void eventSearchRequest_clampsOversizedSizeTo100() {
        EventSearchRequest request = new EventSearchRequest(
                null, null, null, null, null, null, null, null,
                null, null, 0, 500
        );

        assertEquals(100, request.size(),
                "Size above 100 should be clamped to 100");
    }

    @Test
    @DisplayName("EventSearchRequest - clamps zero size to default 20")
    void eventSearchRequest_clampsZeroSizeToDefault() {
        EventSearchRequest request = new EventSearchRequest(
                null, null, null, null, null, null, null, null,
                null, null, 0, 0
        );

        assertEquals(20, request.size(),
                "Size of 0 should be clamped to 20 (default)");
    }

    @Test
    @DisplayName("EventSearchRequest - nullable fields remain null when not provided")
    void eventSearchRequest_nullableFieldsRemainNull() {
        EventSearchRequest request = new EventSearchRequest(
                null, null, null, null, null, null, null, null,
                null, null, null, null
        );

        assertNull(request.q());
        assertNull(request.category());
        assertNull(request.tags());
        assertNull(request.city());
        assertNull(request.dateFrom());
        assertNull(request.dateTo());
        assertNull(request.minPrice());
        assertNull(request.maxPrice());
    }
}
