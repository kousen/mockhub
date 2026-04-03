package com.mockhub.admin.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mockhub.admin.dto.AdminEventDto;
import com.mockhub.common.dto.PagedResponse;
import com.mockhub.common.exception.ResourceNotFoundException;
import com.mockhub.event.dto.CategoryDto;
import com.mockhub.event.dto.EventCreateRequest;
import com.mockhub.event.dto.EventDto;
import com.mockhub.event.dto.TagDto;
import com.mockhub.event.entity.Event;
import com.mockhub.event.repository.EventRepository;
import com.mockhub.event.service.EventService;
import com.mockhub.ticket.entity.Ticket;
import com.mockhub.ticket.repository.TicketRepository;
import com.mockhub.venue.dto.VenueSummaryDto;
import com.mockhub.venue.entity.Seat;
import com.mockhub.venue.entity.SeatRow;
import com.mockhub.venue.entity.Section;
import com.mockhub.venue.entity.Venue;

@Service
public class AdminEventService {

    private static final Logger log = LoggerFactory.getLogger(AdminEventService.class);

    private final EventRepository eventRepository;
    private final EventService eventService;
    private final TicketRepository ticketRepository;

    public AdminEventService(EventRepository eventRepository,
                             EventService eventService,
                             TicketRepository ticketRepository) {
        this.eventRepository = eventRepository;
        this.eventService = eventService;
        this.ticketRepository = ticketRepository;
    }

    @Transactional(readOnly = true)
    public PagedResponse<AdminEventDto> getAllEvents(Pageable pageable) {
        Page<Event> eventPage = eventRepository.findAll(pageable);

        List<AdminEventDto> content = eventPage.getContent().stream()
                .map(this::toAdminEventDto)
                .toList();

        return new PagedResponse<>(
                content,
                eventPage.getNumber(),
                eventPage.getSize(),
                eventPage.getTotalElements(),
                eventPage.getTotalPages()
        );
    }

    @Transactional
    public EventDto createEvent(EventCreateRequest request) {
        return eventService.createEvent(request);
    }

    @Transactional
    public EventDto updateEvent(Long id, EventCreateRequest request) {
        return eventService.updateEvent(id, request);
    }

    @Transactional
    public void deleteEvent(Long id) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Event", "id", id));

        event.setStatus("CANCELLED");
        eventRepository.save(event);
        log.info("Admin cancelled event {} ({})", id, event.getName());
    }

    @Transactional
    public int generateTicketsForEvent(Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event", "id", eventId));

        Venue venue = event.getVenue();
        List<Ticket> tickets = new ArrayList<>();

        for (Section section : venue.getSections()) {
            if ("GENERAL_ADMISSION".equals(section.getSectionType())) {
                for (int i = 0; i < section.getCapacity(); i++) {
                    Ticket ticket = new Ticket();
                    ticket.setEvent(event);
                    ticket.setSection(section);
                    ticket.setTicketType("GENERAL_ADMISSION");
                    ticket.setFaceValue(event.getBasePrice());
                    ticket.setStatus("AVAILABLE");
                    ticket.setBarcode(generateBarcode());
                    tickets.add(ticket);
                }
            } else {
                for (SeatRow seatRow : section.getSeatRows()) {
                    for (Seat seat : seatRow.getSeats()) {
                        Ticket ticket = new Ticket();
                        ticket.setEvent(event);
                        ticket.setSection(section);
                        ticket.setSeat(seat);
                        ticket.setTicketType("RESERVED");
                        ticket.setFaceValue(event.getBasePrice());
                        ticket.setStatus("AVAILABLE");
                        ticket.setBarcode(generateBarcode());
                        tickets.add(ticket);
                    }
                }
            }
        }

        List<Ticket> savedTickets = ticketRepository.saveAll(tickets);
        int ticketCount = savedTickets.size();

        event.setTotalTickets(ticketCount);
        event.setAvailableTickets(ticketCount);
        eventRepository.save(event);

        log.info("Generated {} tickets for event {} ({})", ticketCount, eventId, event.getName());
        return ticketCount;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getSpotifyStatusForEvents(String nameQuery) {
        List<Map<String, Object>> results = new ArrayList<>();
        List<Event> events = eventRepository.findAll().stream()
                .filter(event -> event.getName().toLowerCase().contains(nameQuery.toLowerCase()))
                .limit(20)
                .toList();
        for (Event event : events) {
            results.add(Map.of(
                    "id", event.getId(),
                    "name", event.getName(),
                    "spotifyArtistId", event.getSpotifyArtistId() != null ? event.getSpotifyArtistId() : "NULL",
                    "spotifyArtistIdLength", event.getSpotifyArtistId() != null ? event.getSpotifyArtistId().length() : -1,
                    "ticketmasterEventId", event.getTicketmasterEventId() != null ? event.getTicketmasterEventId() : "NULL",
                    "artistName", event.getArtistName() != null ? event.getArtistName() : "NULL"));
        }
        return results;
    }

    @Transactional
    public Map<String, Integer> activateTicketmasterEvents() {
        int deactivated = eventRepository.deactivateSeedEvents();
        int featured = eventRepository.featureTicketmasterEvents();
        int completed = eventRepository.completePastTicketmasterEvents(java.time.Instant.now());
        log.info("Ticketmaster activation: {} seed deactivated, {} TM featured, {} past completed",
                deactivated, featured, completed);
        return Map.of(
                "seedEventsDeactivated", deactivated,
                "ticketmasterEventsFeatured", featured,
                "pastEventsCompleted", completed);
    }

    private String generateBarcode() {
        return "MH-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
    }

    private AdminEventDto toAdminEventDto(Event event) {
        Venue venue = event.getVenue();
        VenueSummaryDto venueSummary = new VenueSummaryDto(
                venue.getId(),
                venue.getName(),
                venue.getSlug(),
                venue.getCity(),
                venue.getState(),
                venue.getVenueType(),
                venue.getCapacity(),
                venue.getImageUrl()
        );

        CategoryDto categoryDto = new CategoryDto(
                event.getCategory().getId(),
                event.getCategory().getName(),
                event.getCategory().getSlug(),
                event.getCategory().getIcon(),
                event.getCategory().getSortOrder()
        );

        List<TagDto> tagDtos = event.getTags().stream()
                .map(tag -> new TagDto(tag.getId(), tag.getName(), tag.getSlug()))
                .toList();

        int soldTickets = event.getTotalTickets() - event.getAvailableTickets();

        BigDecimal revenue = BigDecimal.ZERO;

        return new AdminEventDto(
                event.getId(),
                event.getName(),
                event.getSlug(),
                event.getDescription(),
                event.getArtistName(),
                event.getEventDate(),
                event.getDoorsOpenAt(),
                event.getStatus(),
                event.getBasePrice(),
                event.getMinPrice(),
                event.getMaxPrice(),
                event.getTotalTickets(),
                event.getAvailableTickets(),
                event.isFeatured(),
                venueSummary,
                categoryDto,
                tagDtos,
                null,
                soldTickets,
                revenue
        );
    }
}
