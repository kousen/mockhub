package com.mockhub.ticket.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.mockhub.common.exception.ConflictException;
import com.mockhub.common.exception.ResourceNotFoundException;
import com.mockhub.event.entity.Event;
import com.mockhub.event.repository.EventRepository;
import com.mockhub.ticket.dto.TicketDto;
import com.mockhub.ticket.entity.Ticket;
import com.mockhub.ticket.repository.TicketRepository;
import com.mockhub.venue.dto.SectionAvailabilityDto;
import com.mockhub.venue.entity.Section;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TicketServiceTest {

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private EventRepository eventRepository;

    @InjectMocks
    private TicketService ticketService;

    private Ticket testTicket;
    private Event testEvent;
    private Section testSection;

    @BeforeEach
    void setUp() {
        testEvent = new Event();
        testEvent.setId(1L);
        testEvent.setName("Test Event");

        testSection = new Section();
        testSection.setId(1L);
        testSection.setName("Section A");

        testTicket = new Ticket();
        testTicket.setId(1L);
        testTicket.setEvent(testEvent);
        testTicket.setSection(testSection);
        testTicket.setTicketType("GENERAL_ADMISSION");
        testTicket.setFaceValue(new BigDecimal("50.00"));
        testTicket.setStatus("AVAILABLE");
    }

    @Test
    @DisplayName("getByEvent - given event with tickets - returns ticket DTOs")
    void getByEvent_givenEventWithTickets_returnsTicketDtos() {
        when(ticketRepository.findByEventId(1L)).thenReturn(List.of(testTicket));

        List<TicketDto> result = ticketService.getByEvent(1L);

        assertNotNull(result, "Result should not be null");
        assertEquals(1, result.size(), "Should return one ticket");
        assertEquals("Section A", result.get(0).sectionName(), "Section name should match");
    }

    @Test
    @DisplayName("getAvailableByEvent - given available tickets - returns only available ones")
    void getAvailableByEvent_givenAvailableTickets_returnsOnlyAvailable() {
        when(ticketRepository.findByEventIdAndStatus(1L, "AVAILABLE"))
                .thenReturn(List.of(testTicket));

        List<TicketDto> result = ticketService.getAvailableByEvent(1L);

        assertEquals(1, result.size(), "Should return one available ticket");
        assertEquals("AVAILABLE", result.get(0).status(), "Status should be AVAILABLE");
    }

    @Test
    @DisplayName("reserveTicket - given available ticket - sets status to RESERVED")
    void reserveTicket_givenAvailableTicket_setsStatusToReserved() {
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(testTicket));
        when(ticketRepository.save(any(Ticket.class))).thenReturn(testTicket);

        TicketDto result = ticketService.reserveTicket(1L);

        assertNotNull(result, "Result should not be null");
        verify(ticketRepository).save(any(Ticket.class));
    }

    @Test
    @DisplayName("reserveTicket - given already reserved ticket - throws ConflictException")
    void reserveTicket_givenAlreadyReservedTicket_throwsConflictException() {
        testTicket.setStatus("RESERVED");
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(testTicket));

        assertThrows(ConflictException.class,
                () -> ticketService.reserveTicket(1L),
                "Should throw ConflictException for already reserved ticket");
    }

    @Test
    @DisplayName("reserveTicket - given nonexistent ticket - throws ResourceNotFoundException")
    void reserveTicket_givenNonexistentTicket_throwsResourceNotFoundException() {
        when(ticketRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> ticketService.reserveTicket(999L),
                "Should throw ResourceNotFoundException for unknown ticket");
    }

    @Test
    @DisplayName("releaseTicket - given reserved ticket - sets status to AVAILABLE")
    void releaseTicket_givenReservedTicket_setsStatusToAvailable() {
        testTicket.setStatus("RESERVED");
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(testTicket));
        when(ticketRepository.save(any(Ticket.class))).thenReturn(testTicket);

        TicketDto result = ticketService.releaseTicket(1L);

        assertNotNull(result, "Result should not be null");
        verify(ticketRepository).save(any(Ticket.class));
    }

    @Test
    @DisplayName("releaseTicket - given non-reserved ticket - throws ConflictException")
    void releaseTicket_givenNonReservedTicket_throwsConflictException() {
        testTicket.setStatus("AVAILABLE");
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(testTicket));

        assertThrows(ConflictException.class,
                () -> ticketService.releaseTicket(1L),
                "Should throw ConflictException for non-reserved ticket");
    }

    @Test
    @DisplayName("getSectionAvailability - given event with SVG data - returns SVG fields in DTO")
    void getSectionAvailability_givenEventWithSvgData_returnsSvgFieldsInDto() {
        when(eventRepository.findBySlug("test-event")).thenReturn(Optional.of(testEvent));

        Object[] row = new Object[]{
                1L, "Floor", "FLOOR",
                100L, 75L,
                new BigDecimal("50.00"), new BigDecimal("150.00"),
                "#FF4444",
                "floor",
                new BigDecimal("50.00"), new BigDecimal("45.00"),
                new BigDecimal("500.00"), new BigDecimal("80.00")
        };
        List<Object[]> rows = java.util.Collections.singletonList(row);
        when(ticketRepository.findSectionAvailabilityByEventId(anyLong()))
                .thenReturn(rows);

        List<SectionAvailabilityDto> result = ticketService.getSectionAvailability("test-event");

        assertEquals(1, result.size());
        SectionAvailabilityDto dto = result.get(0);
        assertEquals(1L, dto.sectionId());
        assertEquals("Floor", dto.sectionName());
        assertEquals("#FF4444", dto.colorHex());
        assertEquals("floor", dto.svgPathId());
        assertEquals(new BigDecimal("50.00"), dto.svgX());
        assertEquals(new BigDecimal("45.00"), dto.svgY());
        assertEquals(new BigDecimal("500.00"), dto.svgWidth());
        assertEquals(new BigDecimal("80.00"), dto.svgHeight());
    }
}
