package com.mockhub.ticket.dto;

import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ListingSearchCriteriaTest {

    @Test
    @DisplayName("compact constructor - null dateFrom - defaults to now")
    void compactConstructor_nullDateFrom_defaultsToNow() {
        Instant before = Instant.now();
        ListingSearchCriteria criteria = new ListingSearchCriteria(
                null, null, null, null, null, null, null, null, 10);
        Instant after = Instant.now();

        assertNotNull(criteria.dateFrom(), "dateFrom should not be null");
        // dateFrom should be between before and after (inclusive)
        assert !criteria.dateFrom().isBefore(before);
        assert !criteria.dateFrom().isAfter(after);
    }

    @Test
    @DisplayName("compact constructor - zero limit - defaults to 10")
    void compactConstructor_zeroLimit_defaultsTo10() {
        ListingSearchCriteria criteria = new ListingSearchCriteria(
                null, null, null, null, null, null, null, null, 0);

        assertEquals(10, criteria.limit());
    }

    @Test
    @DisplayName("compact constructor - negative limit - defaults to 10")
    void compactConstructor_negativeLimit_defaultsTo10() {
        ListingSearchCriteria criteria = new ListingSearchCriteria(
                null, null, null, null, null, null, null, null, -5);

        assertEquals(10, criteria.limit());
    }

    @Test
    @DisplayName("compact constructor - valid values - preserves them")
    void compactConstructor_validValues_preservesThem() {
        Instant dateFrom = Instant.parse("2026-04-01T00:00:00Z");
        ListingSearchCriteria criteria = new ListingSearchCriteria(
                "rock", "concerts", "chicago", null, null, "floor",
                dateFrom, null, 25);

        assertEquals("rock", criteria.query());
        assertEquals("concerts", criteria.categorySlug());
        assertEquals("chicago", criteria.city());
        assertEquals("floor", criteria.section());
        assertEquals(dateFrom, criteria.dateFrom());
        assertEquals(25, criteria.limit());
    }
}
