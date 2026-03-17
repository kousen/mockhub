package com.mockhub.ticket.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.mockhub.ticket.entity.Listing;

@Repository
public interface ListingRepository extends JpaRepository<Listing, Long> {

    List<Listing> findByEventIdAndStatus(Long eventId, String status);

    List<Listing> findByEventId(Long eventId);
}
