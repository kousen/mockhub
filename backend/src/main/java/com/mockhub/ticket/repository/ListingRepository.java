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

    @Query(value = """
            SELECT l.id FROM listings l
            JOIN tickets t ON t.id = l.ticket_id
            JOIN sections s ON s.id = t.section_id
            JOIN events e ON e.id = l.event_id
            JOIN venues v ON v.id = e.venue_id
            JOIN categories c ON c.id = e.category_id
            WHERE l.status = 'ACTIVE'
            AND e.id IN (
                SELECT ev.id FROM events ev
                JOIN venues ven ON ven.id = ev.venue_id
                JOIN categories cat ON cat.id = ev.category_id
                WHERE (CAST(:query AS varchar) IS NULL OR LOWER(ev.name) LIKE '%' || CAST(:query AS varchar) || '%'
                     OR LOWER(ev.artist_name) LIKE '%' || CAST(:query AS varchar) || '%')
                AND (CAST(:categorySlug AS varchar) IS NULL OR cat.slug = CAST(:categorySlug AS varchar))
                AND (CAST(:city AS varchar) IS NULL OR LOWER(ven.city) = CAST(:city AS varchar))
                AND ev.event_date > CAST(:dateFrom AS timestamptz)
                AND (CAST(:dateTo AS timestamptz) IS NULL OR ev.event_date <= CAST(:dateTo AS timestamptz))
            )
            AND (CAST(:minPrice AS numeric) IS NULL OR l.computed_price >= CAST(:minPrice AS numeric))
            AND (CAST(:maxPrice AS numeric) IS NULL OR l.computed_price <= CAST(:maxPrice AS numeric))
            AND (CAST(:section AS varchar) IS NULL OR LOWER(s.name) = CAST(:section AS varchar))
            ORDER BY l.computed_price ASC
            """, nativeQuery = true)
    @SuppressWarnings("java:S107") // Repository query params map directly to SQL named parameters
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
