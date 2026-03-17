package com.mockhub.pricing.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mockhub.common.exception.ResourceNotFoundException;
import com.mockhub.event.entity.Event;
import com.mockhub.event.repository.EventRepository;
import com.mockhub.pricing.dto.PriceHistoryDto;
import com.mockhub.pricing.entity.PriceHistory;
import com.mockhub.pricing.repository.PriceHistoryRepository;

@Service
public class PriceHistoryService {

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

        return getByEventId(event.getId());
    }

    @Transactional(readOnly = true)
    public List<PriceHistoryDto> getByEventId(Long eventId) {
        List<PriceHistory> history = priceHistoryRepository
                .findByEventIdOrderByRecordedAtDesc(eventId);

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
