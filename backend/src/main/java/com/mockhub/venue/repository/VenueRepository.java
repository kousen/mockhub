package com.mockhub.venue.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.mockhub.venue.entity.Venue;

public interface VenueRepository extends JpaRepository<Venue, Long> {

    Optional<Venue> findBySlug(String slug);

    // Hibernate 7 (Spring Boot 4.1) cannot plan multi-level nested bag fetches
    // (sections.seatRows.seats) on derived queries — it throws "Could not
    // generate fetch" and broke the nightly Ticketmaster sync (#294). Fetch one
    // level; seatRows/seats lazy-load inside the sync's @Transactional boundary.
    @EntityGraph(attributePaths = {"sections"})
    Optional<Venue> findByTicketmasterVenueId(String ticketmasterVenueId);

    @EntityGraph(attributePaths = {"sections"})
    Optional<Venue> findByNameAndCity(String name, String city);

    Page<Venue> findByCity(String city, Pageable pageable);
}
