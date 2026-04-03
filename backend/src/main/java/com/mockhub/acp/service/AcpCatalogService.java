package com.mockhub.acp.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mockhub.acp.dto.AcpCatalogItem;
import com.mockhub.acp.dto.AcpListingItem;
import com.mockhub.common.dto.PagedResponse;
import com.mockhub.event.dto.EventSearchRequest;
import com.mockhub.event.dto.EventSummaryDto;
import com.mockhub.event.service.EventService;
import java.time.Instant;

import com.mockhub.ticket.entity.Listing;
import com.mockhub.ticket.repository.ListingRepository;

@Service
public class AcpCatalogService {

    private static final String STATUS_ACTIVE = "ACTIVE";

    private final EventService eventService;
    private final ListingRepository listingRepository;

    public AcpCatalogService(EventService eventService, ListingRepository listingRepository) {
        this.eventService = eventService;
        this.listingRepository = listingRepository;
    }

    @Transactional(readOnly = true)
    public PagedResponse<AcpCatalogItem> getCatalog(String query, String category,
                                                     String city, int page, int size) {
        EventSearchRequest searchRequest = new EventSearchRequest(
                query, category, null, city, null, null, null, null, STATUS_ACTIVE, "eventDate", page, size
        );

        PagedResponse<EventSummaryDto> eventPage = eventService.listEvents(searchRequest);

        List<AcpCatalogItem> catalogItems = eventPage.content().stream()
                .map(event -> new AcpCatalogItem(
                        event.slug(),
                        event.name(),
                        event.artistName() != null ? event.artistName() : event.name(),
                        event.categoryName(),
                        event.venueName(),
                        event.city(),
                        event.eventDate(),
                        event.minPrice(),
                        event.minPrice(),
                        event.availableTickets(),
                        "/events/" + event.slug()
                ))
                .toList();

        return new PagedResponse<>(
                catalogItems,
                eventPage.page(),
                eventPage.size(),
                eventPage.totalElements(),
                eventPage.totalPages()
        );
    }

    @Transactional(readOnly = true)
    public PagedResponse<AcpListingItem> getListings(String query, String category, String city,
                                                      Instant dateFrom, Instant dateTo,
                                                      BigDecimal minPrice, BigDecimal maxPrice,
                                                      String section, int page, int size) {
        List<EventSummaryDto> allEvents = new ArrayList<>();
        int eventPage = 0;
        int eventPageSize = 100;
        PagedResponse<EventSummaryDto> eventBatch;
        do {
            EventSearchRequest searchRequest = new EventSearchRequest(
                    query, category, null, city,
                    dateFrom, dateTo, minPrice, maxPrice,
                    STATUS_ACTIVE, "eventDate", eventPage, eventPageSize
            );
            eventBatch = eventService.listEvents(searchRequest);
            allEvents.addAll(eventBatch.content());
            eventPage++;
        } while (eventPage < eventBatch.totalPages());

        List<AcpListingItem> allListings = new ArrayList<>();

        for (EventSummaryDto event : allEvents) {
            List<Listing> eventListings = listingRepository.findByEventIdAndStatus(event.id(), STATUS_ACTIVE);
            for (Listing listing : eventListings) {
                if (!matchesFilters(listing, minPrice, maxPrice, section)) {
                    continue;
                }

                String rowLabel = null;
                String seatNumber = null;
                if (listing.getTicket().getSeat() != null) {
                    rowLabel = listing.getTicket().getSeat().getRow().getRowLabel();
                    seatNumber = listing.getTicket().getSeat().getSeatNumber();
                }

                allListings.add(new AcpListingItem(
                        listing.getId(),
                        event.slug(),
                        event.name(),
                        event.slug(),
                        event.artistName() != null ? event.artistName() : event.name(),
                        event.categoryName(),
                        event.venueName(),
                        event.city(),
                        event.eventDate(),
                        listing.getTicket().getSection().getName(),
                        rowLabel,
                        seatNumber,
                        listing.getComputedPrice(),
                        "/events/" + event.slug()
                ));
            }
        }

        List<AcpListingItem> sortedListings = allListings.stream()
                .sorted(Comparator.comparing(AcpListingItem::price))
                .toList();

        int safePage = Math.max(page, 0);
        int safeSize = size <= 0 ? 20 : Math.min(size, 100);
        int start = Math.min(safePage * safeSize, sortedListings.size());
        int end = Math.min(start + safeSize, sortedListings.size());
        int totalPages = sortedListings.isEmpty() ? 0 : (int) Math.ceil((double) sortedListings.size() / safeSize);

        return new PagedResponse<>(
                sortedListings.subList(start, end),
                safePage,
                safeSize,
                sortedListings.size(),
                totalPages
        );
    }

    private boolean matchesFilters(Listing listing, BigDecimal minPrice,
                                   BigDecimal maxPrice, String section) {
        boolean priceAboveMin = minPrice == null || listing.getComputedPrice().compareTo(minPrice) >= 0;
        boolean priceBelowMax = maxPrice == null || listing.getComputedPrice().compareTo(maxPrice) <= 0;
        boolean sectionMatches = section == null || section.isBlank()
                || section.strip().equalsIgnoreCase(listing.getTicket().getSection().getName());
        return priceAboveMin && priceBelowMax && sectionMatches;
    }
}
