package com.mockhub.ticket.repository;

import java.time.Instant;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.mockhub.ticket.entity.Listing;

public interface ListingRepository extends JpaRepository<Listing, Long>, JpaSpecificationExecutor<Listing> {

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

    @Query("""
            SELECT l FROM Listing l
            JOIN FETCH l.ticket t
            JOIN FETCH t.section s
            LEFT JOIN FETCH t.seat seat
            LEFT JOIN FETCH seat.row r
            LEFT JOIN FETCH l.seller seller
            WHERE l.event.id = :eventId AND l.status = :status
            ORDER BY l.computedPrice ASC
            """)
    List<Listing> findByEventIdAndStatusOrderByPrice(
            @Param("eventId") Long eventId,
            @Param("status") String status,
            Pageable pageable);

    List<Listing> findByEventId(Long eventId);

    @Query("""
            SELECT l FROM Listing l
            JOIN FETCH l.event e
            WHERE l.status = 'ACTIVE' AND e.status = 'ACTIVE' AND e.eventDate > :now
            ORDER BY l.computedPrice ASC, l.id ASC
            """)
    List<Listing> findCheapestActiveListingsForFutureEvents(@Param("now") Instant now, Pageable pageable);

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

    long countByEventIdAndStatus(Long eventId, String status);

    long countBySellerIdAndStatus(Long sellerId, String status);

    boolean existsByTicketIdAndStatus(Long ticketId, String status);

    @Query("""
            SELECT l FROM Listing l
            JOIN FETCH l.ticket t
            JOIN FETCH t.section s
            LEFT JOIN FETCH t.seat seat
            LEFT JOIN FETCH seat.row r
            JOIN FETCH l.event e
            JOIN FETCH e.venue v
            JOIN FETCH e.category c
            LEFT JOIN FETCH l.seller seller
            WHERE l.id IN :ids AND l.status = 'ACTIVE'
            ORDER BY l.computedPrice ASC
            """)
    List<Listing> findByIdsWithDetails(@Param("ids") List<Long> ids);

    @Query("""
            SELECT l FROM Listing l
            JOIN FETCH l.ticket t
            WHERE l.status = 'ACTIVE' AND l.expiresAt IS NOT NULL AND l.expiresAt < :now
            """)
    List<Listing> findActiveListingsPastDeadline(@Param("now") Instant now);

    @Query("""
            SELECT l FROM Listing l
            JOIN FETCH l.ticket t
            WHERE l.status = 'ACTIVE' AND l.event.id IN (
                SELECT e.id FROM Event e WHERE e.eventDate < :now
            )
            """)
    List<Listing> findActiveListingsForPastEvents(@Param("now") Instant now);

    @Query("""
            SELECT l FROM Listing l
            JOIN FETCH l.ticket t
            WHERE l.status = 'ACTIVE' AND l.event.status <> 'ACTIVE'
                AND t.status = 'LISTED'
            """)
    List<Listing> findActiveListingsForInactiveEvents();

    @Modifying
    @Query("""
            DELETE FROM Listing l
            WHERE l.event.id IN (SELECT e.id FROM Event e WHERE e.status <> 'ACTIVE')
                AND NOT EXISTS (SELECT 1 FROM OrderItem oi WHERE oi.listing = l)
                AND NOT EXISTS (SELECT 1 FROM CartItem ci WHERE ci.listing = l)
            """)
    int deleteOrphanedForInactiveEvents();
}
