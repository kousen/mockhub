package com.mockhub.ticket.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.mockhub.ticket.entity.Listing;

@Repository
public interface ListingRepository extends JpaRepository<Listing, Long> {

    @Query("""
            SELECT l FROM Listing l
            JOIN FETCH l.ticket t
            JOIN FETCH t.section s
            LEFT JOIN FETCH t.seat seat
            LEFT JOIN FETCH seat.row r
            WHERE l.event.id = :eventId AND l.status = :status
            ORDER BY s.name ASC, r.rowLabel ASC, seat.seatNumber ASC
            """)
    List<Listing> findByEventIdAndStatus(Long eventId, String status);

    List<Listing> findByEventId(Long eventId);

    @Query("""
            SELECT l FROM Listing l
            JOIN FETCH l.ticket t
            JOIN FETCH l.event e
            JOIN FETCH e.venue v
            JOIN FETCH t.section s
            LEFT JOIN FETCH t.seat seat
            LEFT JOIN FETCH seat.row r
            WHERE l.seller.id = :sellerId AND l.status = :status
            ORDER BY l.createdAt DESC
            """)
    List<Listing> findBySellerIdAndStatus(
            @Param("sellerId") Long sellerId,
            @Param("status") String status);

    @Query("""
            SELECT l FROM Listing l
            JOIN FETCH l.ticket t
            JOIN FETCH l.event e
            JOIN FETCH e.venue v
            JOIN FETCH t.section s
            LEFT JOIN FETCH t.seat seat
            LEFT JOIN FETCH seat.row r
            WHERE l.seller.id = :sellerId
            ORDER BY l.createdAt DESC
            """)
    List<Listing> findBySellerId(@Param("sellerId") Long sellerId);

    long countBySellerIdAndStatus(Long sellerId, String status);

    boolean existsByTicketIdAndStatus(Long ticketId, String status);
}
