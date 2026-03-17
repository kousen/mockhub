package com.mockhub.pricing.repository;

import java.time.Instant;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.mockhub.pricing.entity.PriceHistory;

@Repository
public interface PriceHistoryRepository extends JpaRepository<PriceHistory, Long> {

    List<PriceHistory> findByEventIdOrderByRecordedAtDesc(Long eventId);

    List<PriceHistory> findByEventIdAndRecordedAtBetween(Long eventId, Instant start, Instant end);
}
