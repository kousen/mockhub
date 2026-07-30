package com.mockhub.venue.repository;

import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.mockhub.AbstractIntegrationTest;
import com.mockhub.venue.entity.Venue;

/**
 * Executes the venue lookup queries against real PostgreSQL/Hibernate.
 *
 * The nightly Ticketmaster sync broke silently after the Spring Boot 4.1
 * upgrade (#294): Hibernate 7 rejects the multi-level nested bag fetch the
 * old @EntityGraph declared ("Could not generate fetch : Venue(v) ->
 * sections"), and the mocked sync tests never execute real query
 * generation. These tests fail on any EntityGraph Hibernate can't plan.
 */
@DisplayName("VenueRepository")
class VenueRepositoryIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private VenueRepository venueRepository;

    @Test
    @DisplayName("findByTicketmasterVenueId - executes real query without fetch error")
    void findByTicketmasterVenueId_executesRealQuery() {
        Optional<Venue> result = venueRepository.findByTicketmasterVenueId("tm-venue-does-not-exist");

        Assertions.assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("findByNameAndCity - executes real query without fetch error")
    void findByNameAndCity_executesRealQuery() {
        Optional<Venue> result = venueRepository.findByNameAndCity("No Such Venue", "Nowhere");

        Assertions.assertTrue(result.isEmpty());
    }
}
