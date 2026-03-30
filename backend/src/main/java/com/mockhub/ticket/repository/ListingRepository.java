package com.mockhub.ticket.repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.springframework.data.domain.Pageable;
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
            SELECT l.id FROM Listing l
            JOIN l.ticket t
            JOIN t.section s
            JOIN l.event e
            JOIN e.venue v
            JOIN e.category c
            WHERE l.status = 'ACTIVE'
            AND l.event.id IN (
                SELECT ev.id FROM Event ev
                JOIN ev.venue ven
                JOIN ev.category cat
                WHERE (:query IS NULL OR LOWER(ev.name) LIKE CONCAT('%', :query, '%')
                     OR LOWER(ev.artistName) LIKE CONCAT('%', :query, '%'))
                AND (:categorySlug IS NULL OR cat.slug = :categorySlug)
                AND (:city IS NULL OR LOWER(ven.city) = :city)
                AND ev.eventDate > :dateFrom
                AND (:dateTo IS NULL OR ev.eventDate <= :dateTo)
            )
            AND (:minPrice IS NULL OR l.computedPrice >= :minPrice)
            AND (:maxPrice IS NULL OR l.computedPrice <= :maxPrice)
            AND (:section IS NULL OR LOWER(s.name) = :section)
            ORDER BY l.computedPrice ASC
            """)
    @SuppressWarnings("java:S107") // Repository query params map directly to JPQL named parameters
    List<Long> searchActiveListingIds(
            @Param("query") String query,
            @Param("categorySlug") String categorySlug,
            @Param("city") String city,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            @Param("section") String section,
            @Param("dateFrom") Instant dateFrom,
            @Param("dateTo") Instant dateTo,
            org.springframework.data.domain.Pageable pageable);

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
}
