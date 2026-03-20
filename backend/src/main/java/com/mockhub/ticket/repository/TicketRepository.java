package com.mockhub.ticket.repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.mockhub.ticket.entity.Ticket;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long> {

    List<Ticket> findByEventId(Long eventId);

    List<Ticket> findByEventIdAndStatus(Long eventId, String status);

    long countByEventIdAndStatus(Long eventId, String status);

    @Query("""
            SELECT t FROM Ticket t
            JOIN t.section s
            JOIN t.seat seat
            JOIN seat.row r
            WHERE t.event.id = :eventId
            AND s.name = :sectionName
            AND r.rowLabel = :rowLabel
            AND seat.seatNumber = :seatNumber
            """)
    Optional<Ticket> findByEventIdAndSectionAndRowAndSeat(
            @Param("eventId") Long eventId,
            @Param("sectionName") String sectionName,
            @Param("rowLabel") String rowLabel,
            @Param("seatNumber") String seatNumber);

    @Query("SELECT t.section.id, t.section.name, t.section.sectionType, "
            + "COUNT(t), "
            + "SUM(CASE WHEN t.status = 'AVAILABLE' THEN 1 ELSE 0 END), "
            + "MIN(t.faceValue), MAX(t.faceValue), t.section.colorHex "
            + "FROM Ticket t WHERE t.event.id = :eventId "
            + "GROUP BY t.section.id, t.section.name, t.section.sectionType, t.section.colorHex, t.section.sortOrder "
            + "ORDER BY t.section.sortOrder")
    List<Object[]> findSectionAvailabilityByEventId(@Param("eventId") Long eventId);
}
