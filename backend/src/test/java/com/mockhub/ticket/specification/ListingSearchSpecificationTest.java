package com.mockhub.ticket.specification;

import java.math.BigDecimal;
import java.time.Instant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.domain.Specification;

import com.mockhub.ticket.dto.ListingSearchCriteria;
import com.mockhub.ticket.entity.Listing;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Verifies that ListingSearchSpecification builds a non-null Specification
 * for all parameter combinations. Actual SQL correctness is validated by
 * FindTicketsQueryIntegrationTest against real PostgreSQL.
 */
class ListingSearchSpecificationTest {

    @Test
    @DisplayName("fromCriteria - all null params - returns non-null specification")
    void fromCriteria_allNullParams_returnsNonNullSpec() {
        ListingSearchCriteria criteria = new ListingSearchCriteria(
                null, null, null, null, null, null, null, null, 10);

        Specification<Listing> spec = ListingSearchSpecification.fromCriteria(criteria);

        assertNotNull(spec, "Specification should not be null");
    }

    @Test
    @DisplayName("fromCriteria - all params provided - returns non-null specification")
    void fromCriteria_allParamsProvided_returnsNonNullSpec() {
        ListingSearchCriteria criteria = new ListingSearchCriteria(
                "rock concert", "concerts", "chicago",
                new BigDecimal("50.00"), new BigDecimal("200.00"), "orchestra",
                Instant.now(), Instant.now().plusSeconds(86400), 20);

        Specification<Listing> spec = ListingSearchSpecification.fromCriteria(criteria);

        assertNotNull(spec, "Specification should not be null");
    }

    @Test
    @DisplayName("fromCriteria - blank strings treated as no-filter - returns non-null specification")
    void fromCriteria_blankStrings_returnsNonNullSpec() {
        ListingSearchCriteria criteria = new ListingSearchCriteria(
                "  ", "  ", "  ", null, null, "  ", null, null, 10);

        Specification<Listing> spec = ListingSearchSpecification.fromCriteria(criteria);

        assertNotNull(spec, "Specification should not be null");
    }
}
