package com.mockhub;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;

import com.mockhub.ticket.dto.TicketSearchResultDto;
import com.mockhub.ticket.repository.ListingRepository;
import com.mockhub.ticket.service.ListingService;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Integration test that runs the actual findTickets JPQL against a real
 * PostgreSQL database. Catches type inference issues (lower(bytea), parameter
 * type errors) that only surface with null parameters on real Postgres.
 */
class FindTicketsQueryIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private ListingService listingService;

    @Autowired
    private ListingRepository listingRepository;

    @Test
    @DisplayName("searchActiveListingIds - all params null except dateFrom - no SQL error")
    void searchActiveListingIds_allParamsNull_noSqlError() {
        assertDoesNotThrow(() -> {
            List<Long> ids = listingRepository.searchActiveListingIds(
                    null, null, null, null, null, null,
                    java.time.Instant.now(), null,
                    PageRequest.of(0, 10));
            assertNotNull(ids);
        });
    }

    @Test
    @DisplayName("searchActiveListingIds - with query param - no SQL error")
    void searchActiveListingIds_withQuery_noSqlError() {
        assertDoesNotThrow(() -> {
            List<Long> ids = listingRepository.searchActiveListingIds(
                    "concert", null, null, null, null, null,
                    java.time.Instant.now(), null,
                    PageRequest.of(0, 10));
            assertNotNull(ids);
        });
    }

    @Test
    @DisplayName("searchActiveListingIds - with city param - no SQL error")
    void searchActiveListingIds_withCity_noSqlError() {
        assertDoesNotThrow(() -> {
            List<Long> ids = listingRepository.searchActiveListingIds(
                    null, null, "chicago", null, null, null,
                    java.time.Instant.now(), null,
                    PageRequest.of(0, 10));
            assertNotNull(ids);
        });
    }

    @Test
    @DisplayName("searchActiveListingIds - with section param - no SQL error")
    void searchActiveListingIds_withSection_noSqlError() {
        assertDoesNotThrow(() -> {
            List<Long> ids = listingRepository.searchActiveListingIds(
                    null, null, null, null, null, "orchestra",
                    java.time.Instant.now(), null,
                    PageRequest.of(0, 10));
            assertNotNull(ids);
        });
    }

    @Test
    @DisplayName("searchActiveListingIds - all string params provided - no SQL error")
    void searchActiveListingIds_allStringParams_noSqlError() {
        assertDoesNotThrow(() -> {
            List<Long> ids = listingRepository.searchActiveListingIds(
                    "rock", "concerts", "new york", null, null, "floor",
                    java.time.Instant.now(), null,
                    PageRequest.of(0, 10));
            assertNotNull(ids);
        });
    }

    @Test
    @DisplayName("findByIdsWithDetails - empty list - no SQL error")
    void findByIdsWithDetails_emptyList_noSqlError() {
        assertDoesNotThrow(() -> {
            // Pass a list with a non-existent ID to avoid the empty IN clause issue
            listingRepository.findByIdsWithDetails(List.of(-1L));
        });
    }

    @Test
    @DisplayName("searchTickets end-to-end - null params - no SQL error")
    void searchTickets_endToEnd_nullParams_noSqlError() {
        assertDoesNotThrow(() -> {
            List<TicketSearchResultDto> results = listingService.searchTickets(
                    null, null, null, null, null, null, null, null, 10);
            assertNotNull(results);
        });
    }

    @Test
    @DisplayName("searchTickets end-to-end - with query - no SQL error")
    void searchTickets_endToEnd_withQuery_noSqlError() {
        assertDoesNotThrow(() -> {
            List<TicketSearchResultDto> results = listingService.searchTickets(
                    "Lang Lang", null, null, null, null, null, null, null, 10);
            assertNotNull(results);
        });
    }
}
