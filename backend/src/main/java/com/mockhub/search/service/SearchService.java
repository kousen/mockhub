package com.mockhub.search.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mockhub.common.dto.PagedResponse;
import com.mockhub.event.dto.EventSummaryDto;
import com.mockhub.event.entity.Event;
import com.mockhub.event.repository.EventRepository;
import com.mockhub.search.dto.SearchResultDto;
import com.mockhub.venue.entity.Venue;

@Service
public class SearchService {

    private final EventRepository eventRepository;

    public SearchService(EventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    @Transactional(readOnly = true)
    public PagedResponse<SearchResultDto> search(String query, int page, int size) {
        if (query == null || query.isBlank()) {
            return new PagedResponse<>(List.of(), page, size, 0, 0);
        }

        int offset = page * size;
        List<Event> events = eventRepository.fullTextSearch(query, size, offset);
        long totalElements = eventRepository.countFullTextSearch(query);
        int totalPages = (int) Math.ceil((double) totalElements / size);

        List<SearchResultDto> results = events.stream()
                .map(event -> new SearchResultDto(toEventSummaryDto(event), 1.0))
                .toList();

        return new PagedResponse<>(results, page, size, totalElements, totalPages);
    }

    @Transactional(readOnly = true)
    public List<String> suggest(String query) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        return eventRepository.suggestEvents(query);
    }

    private EventSummaryDto toEventSummaryDto(Event event) {
        Venue venue = event.getVenue();
        return new EventSummaryDto(
                event.getId(),
                event.getName(),
                event.getSlug(),
                event.getArtistName(),
                venue.getName(),
                venue.getCity(),
                event.getEventDate(),
                event.getMinPrice(),
                event.getAvailableTickets(),
                null,
                event.getCategory().getName(),
                event.isFeatured()
        );
    }
}
