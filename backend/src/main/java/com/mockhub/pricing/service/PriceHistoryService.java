package com.mockhub.pricing.service;

import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mockhub.common.exception.ResourceNotFoundException;
import com.mockhub.event.entity.Event;
import com.mockhub.event.repository.EventRepository;
import com.mockhub.pricing.dto.PriceHistoryDto;
import com.mockhub.pricing.dto.PriceHistoryPageDto;
import com.mockhub.pricing.entity.PriceHistory;
import com.mockhub.pricing.repository.PriceHistoryRepository;

@Service
public class PriceHistoryService {

    public static final int MAX_SNAPSHOT_LIMIT = 500;

    private final PriceHistoryRepository priceHistoryRepository;
    private final EventRepository eventRepository;

    public PriceHistoryService(PriceHistoryRepository priceHistoryRepository,
                               EventRepository eventRepository) {
        this.priceHistoryRepository = priceHistoryRepository;
        this.eventRepository = eventRepository;
    }

    @Transactional(readOnly = true)
    public List<PriceHistoryDto> getByEventSlug(String eventSlug) {
        Event event = eventRepository.findBySlug(eventSlug)
                .orElseThrow(() -> new ResourceNotFoundException("Event", "slug", eventSlug));

        return toHistoryDtos(priceHistoryRepository
                .findByEventIdOrderByRecordedAtDesc(event.getId()));
    }

    /**
     * Bounded variant for agent-facing tools: newest {@code limit} snapshots
     * plus the total count. Limit is clamped to [1, MAX_SNAPSHOT_LIMIT] —
     * popular events accumulate thousands of 5-minute snapshots, which blows
     * agent context windows if returned unbounded.
     */
    @Transactional(readOnly = true)
    public PriceHistoryPageDto getRecentByEventSlug(String eventSlug, int limit) {
        Event event = eventRepository.findBySlug(eventSlug)
                .orElseThrow(() -> new ResourceNotFoundException("Event", "slug", eventSlug));

        int clampedLimit = Math.clamp(limit, 1, MAX_SNAPSHOT_LIMIT);
        List<PriceHistoryDto> snapshots = toHistoryDtos(priceHistoryRepository
                .findByEventIdOrderByRecordedAtDesc(event.getId(), PageRequest.of(0, clampedLimit)));
        long total = priceHistoryRepository.countByEventId(event.getId());
        return new PriceHistoryPageDto(snapshots, snapshots.size(), total, clampedLimit);
    }

    @Transactional(readOnly = true)
    public List<PriceHistoryDto> getByEventId(Long eventId) {
        return toHistoryDtos(priceHistoryRepository
                .findByEventIdOrderByRecordedAtDesc(eventId));
    }

    private List<PriceHistoryDto> toHistoryDtos(List<PriceHistory> history) {
        return history.stream()
                .map(this::toPriceHistoryDto)
                .toList();
    }

    private PriceHistoryDto toPriceHistoryDto(PriceHistory history) {
        return new PriceHistoryDto(
                history.getId(),
                history.getEvent().getId(),
                history.getPrice(),
                history.getMultiplier(),
                history.getSupplyRatio(),
                history.getDemandScore(),
                history.getDaysToEvent(),
                history.getRecordedAt()
        );
    }
}
