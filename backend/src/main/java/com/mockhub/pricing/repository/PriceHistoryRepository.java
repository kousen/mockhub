package com.mockhub.pricing.repository;

import java.time.Instant;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.mockhub.pricing.entity.PriceHistory;

public interface PriceHistoryRepository extends JpaRepository<PriceHistory, Long> {

    List<PriceHistory> findByEventIdOrderByRecordedAtDesc(Long eventId);

    List<PriceHistory> findByEventIdOrderByRecordedAtDesc(Long eventId, Pageable pageable);

    long countByEventId(Long eventId);

    List<PriceHistory> findByEventIdAndRecordedAtBetween(Long eventId, Instant start, Instant end);
}
