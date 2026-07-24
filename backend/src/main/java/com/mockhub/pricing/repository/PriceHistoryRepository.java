package com.mockhub.pricing.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.mockhub.pricing.entity.PriceHistory;

public interface PriceHistoryRepository extends JpaRepository<PriceHistory, Long> {

    @Modifying
    @Query("""
            DELETE FROM PriceHistory ph
            WHERE ph.event.id IN (SELECT e.id FROM Event e WHERE e.status <> 'ACTIVE')
            """)
    int deleteForInactiveEvents();

    @Modifying
    @Query("DELETE FROM PriceHistory ph WHERE ph.recordedAt < :cutoff")
    int deleteRecordedBefore(@Param("cutoff") Instant cutoff);

    List<PriceHistory> findByEventIdOrderByRecordedAtDesc(Long eventId);

    Optional<PriceHistory> findFirstByEventIdOrderByRecordedAtDesc(Long eventId);

    List<PriceHistory> findByEventIdOrderByRecordedAtDesc(Long eventId, Pageable pageable);

    long countByEventId(Long eventId);

    List<PriceHistory> findByEventIdAndRecordedAtBetween(Long eventId, Instant start, Instant end);
}
