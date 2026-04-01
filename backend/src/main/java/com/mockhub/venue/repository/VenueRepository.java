package com.mockhub.venue.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.mockhub.venue.entity.Venue;

@Repository
public interface VenueRepository extends JpaRepository<Venue, Long> {

    Optional<Venue> findBySlug(String slug);

    Optional<Venue> findByTicketmasterVenueId(String ticketmasterVenueId);

    Optional<Venue> findByNameAndCity(String name, String city);

    Page<Venue> findByCity(String city, Pageable pageable);
}
