package com.mockhub;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.mockhub.ticket.dto.ListingSearchCriteria;
import com.mockhub.ticket.dto.TicketSearchResultDto;
import com.mockhub.ticket.service.ListingService;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Integration test that runs the Specification-based listing search against
 * a real PostgreSQL database via Testcontainers. Verifies that dynamic
 * predicate construction works correctly with all parameter combinations.
 */
class FindTicketsQueryIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private ListingService listingService;

    @Test
    @DisplayName("searchTickets - all params null - no SQL error")
    void searchTickets_allParamsNull_noSqlError() {
        assertDoesNotThrow(() -> {
            ListingSearchCriteria criteria = new ListingSearchCriteria(
                    null, null, null, null, null, null, null, null, 10);
            List<TicketSearchResultDto> results = listingService.searchTickets(criteria);
            assertNotNull(results);
        });
    }

    @Test
    @DisplayName("searchTickets - with query param - no SQL error")
    void searchTickets_withQuery_noSqlError() {
        assertDoesNotThrow(() -> {
            ListingSearchCriteria criteria = new ListingSearchCriteria(
                    "concert", null, null, null, null, null, null, null, 10);
            List<TicketSearchResultDto> results = listingService.searchTickets(criteria);
            assertNotNull(results);
        });
    }

    @Test
    @DisplayName("searchTickets - with city param - no SQL error")
    void searchTickets_withCity_noSqlError() {
        assertDoesNotThrow(() -> {
            ListingSearchCriteria criteria = new ListingSearchCriteria(
                    null, null, "chicago", null, null, null, null, null, 10);
            List<TicketSearchResultDto> results = listingService.searchTickets(criteria);
            assertNotNull(results);
        });
    }

    @Test
    @DisplayName("searchTickets - with section param - no SQL error")
    void searchTickets_withSection_noSqlError() {
        assertDoesNotThrow(() -> {
            ListingSearchCriteria criteria = new ListingSearchCriteria(
                    null, null, null, null, null, "orchestra", null, null, 10);
            List<TicketSearchResultDto> results = listingService.searchTickets(criteria);
            assertNotNull(results);
        });
    }

    @Test
    @DisplayName("searchTickets - all string params provided - no SQL error")
    void searchTickets_allStringParams_noSqlError() {
        assertDoesNotThrow(() -> {
            ListingSearchCriteria criteria = new ListingSearchCriteria(
                    "rock", "concerts", "new york", null, null, "floor", null, null, 10);
            List<TicketSearchResultDto> results = listingService.searchTickets(criteria);
            assertNotNull(results);
        });
    }

    @Test
    @DisplayName("searchTickets - with price range - no SQL error")
    void searchTickets_withPriceRange_noSqlError() {
        assertDoesNotThrow(() -> {
            ListingSearchCriteria criteria = new ListingSearchCriteria(
                    null, null, null, new BigDecimal("50.00"), new BigDecimal("200.00"),
                    null, null, null, 10);
            List<TicketSearchResultDto> results = listingService.searchTickets(criteria);
            assertNotNull(results);
        });
    }

    @Test
    @DisplayName("searchTickets - with date range - no SQL error")
    void searchTickets_withDateRange_noSqlError() {
        assertDoesNotThrow(() -> {
            ListingSearchCriteria criteria = new ListingSearchCriteria(
                    null, null, null, null, null, null,
                    Instant.now(), Instant.now().plusSeconds(86400 * 30), 10);
            List<TicketSearchResultDto> results = listingService.searchTickets(criteria);
            assertNotNull(results);
        });
    }

    @Test
    @DisplayName("searchTickets - all filters combined - no SQL error")
    void searchTickets_allFiltersCombined_noSqlError() {
        assertDoesNotThrow(() -> {
            ListingSearchCriteria criteria = new ListingSearchCriteria(
                    "Lang Lang", "concerts", "new york",
                    new BigDecimal("50.00"), new BigDecimal("500.00"), "orchestra",
                    Instant.now(), Instant.now().plusSeconds(86400 * 90), 20);
            List<TicketSearchResultDto> results = listingService.searchTickets(criteria);
            assertNotNull(results);
        });
    }
}
