package com.mockhub.ticket.service;

import java.math.BigDecimal;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mockhub.common.exception.ConflictException;
import com.mockhub.common.exception.ResourceNotFoundException;
import com.mockhub.event.entity.Event;
import com.mockhub.event.repository.EventRepository;
import com.mockhub.ticket.dto.TicketDto;
import com.mockhub.ticket.entity.Ticket;
import com.mockhub.ticket.repository.TicketRepository;
import com.mockhub.venue.dto.SectionAvailabilityDto;

@Service
public class TicketService {

    private static final Logger log = LoggerFactory.getLogger(TicketService.class);
    private static final String STATUS_AVAILABLE = "AVAILABLE";

    private final TicketRepository ticketRepository;
    private final EventRepository eventRepository;

    public TicketService(TicketRepository ticketRepository,
                         EventRepository eventRepository) {
        this.ticketRepository = ticketRepository;
        this.eventRepository = eventRepository;
    }

    @Transactional(readOnly = true)
    public List<TicketDto> getByEvent(Long eventId) {
        List<Ticket> tickets = ticketRepository.findByEventId(eventId);
        return tickets.stream()
                .map(this::toTicketDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TicketDto> getAvailableByEvent(Long eventId) {
        List<Ticket> tickets = ticketRepository.findByEventIdAndStatus(eventId, STATUS_AVAILABLE);
        return tickets.stream()
                .map(this::toTicketDto)
                .toList();
    }

    @Transactional
    public TicketDto reserveTicket(Long ticketId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket", "id", ticketId));

        if (!STATUS_AVAILABLE.equals(ticket.getStatus()) && !"LISTED".equals(ticket.getStatus())) {
            throw new ConflictException("Ticket is not available for reservation");
        }

        ticket.setStatus("RESERVED");
        Ticket saved = ticketRepository.save(ticket);
        log.info("Ticket {} reserved", ticketId);
        return toTicketDto(saved);
    }

    @Transactional
    public TicketDto releaseTicket(Long ticketId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket", "id", ticketId));

        if (!"RESERVED".equals(ticket.getStatus())) {
            throw new ConflictException("Ticket is not in reserved state");
        }

        ticket.setStatus(STATUS_AVAILABLE);
        Ticket saved = ticketRepository.save(ticket);
        log.info("Ticket {} released", ticketId);
        return toTicketDto(saved);
    }

    @Transactional(readOnly = true)
    public List<SectionAvailabilityDto> getSectionAvailability(String eventSlug) {
        Event event = eventRepository.findBySlug(eventSlug)
                .orElseThrow(() -> new ResourceNotFoundException("Event", "slug", eventSlug));

        List<Object[]> results = ticketRepository.findSectionAvailabilityByEventId(event.getId());

        return results.stream()
                .map(row -> new SectionAvailabilityDto(
                        (Long) row[0],
                        (String) row[1],
                        (String) row[2],
                        ((Long) row[3]).intValue(),
                        ((Long) row[4]).intValue(),
                        (BigDecimal) row[5],
                        (BigDecimal) row[6],
                        (String) row[7]
                ))
                .toList();
    }

    private TicketDto toTicketDto(Ticket ticket) {
        String sectionName = ticket.getSection().getName();
        String rowLabel = null;
        String seatNumber = null;

        if (ticket.getSeat() != null) {
            rowLabel = ticket.getSeat().getRow().getRowLabel();
            seatNumber = ticket.getSeat().getSeatNumber();
        }

        return new TicketDto(
                ticket.getId(),
                ticket.getEvent().getId(),
                sectionName,
                rowLabel,
                seatNumber,
                ticket.getTicketType(),
                ticket.getFaceValue(),
                ticket.getStatus()
        );
    }
}
