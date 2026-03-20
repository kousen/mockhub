package com.mockhub.ticket.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.mockhub.auth.repository.UserRepository;
import com.mockhub.common.exception.ConflictException;
import com.mockhub.common.exception.ResourceNotFoundException;
import com.mockhub.event.entity.Event;
import com.mockhub.event.repository.EventRepository;
import com.mockhub.order.repository.OrderItemRepository;
import com.mockhub.ticket.dto.ListingCreateRequest;
import com.mockhub.ticket.dto.ListingDto;
import com.mockhub.auth.entity.User;
import com.mockhub.ticket.entity.Listing;
import com.mockhub.ticket.entity.Ticket;
import com.mockhub.ticket.repository.ListingRepository;
import com.mockhub.ticket.repository.TicketRepository;
import com.mockhub.venue.entity.Seat;
import com.mockhub.venue.entity.SeatRow;
import com.mockhub.venue.entity.Section;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListingServiceTest {

    @Mock
    private ListingRepository listingRepository;

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @InjectMocks
    private ListingService listingService;

    private Event testEvent;
    private Ticket testTicket;
    private Listing testListing;
    private Section testSection;

    @BeforeEach
    void setUp() {
        testEvent = new Event();
        testEvent.setId(1L);
        testEvent.setName("Test Event");
        testEvent.setSlug("test-event");

        testSection = new Section();
        testSection.setId(1L);
        testSection.setName("Floor");

        testTicket = new Ticket();
        testTicket.setId(1L);
        testTicket.setEvent(testEvent);
        testTicket.setSection(testSection);
        testTicket.setTicketType("GENERAL_ADMISSION");
        testTicket.setFaceValue(new BigDecimal("50.00"));
        testTicket.setStatus("AVAILABLE");

        testListing = new Listing();
        testListing.setId(1L);
        testListing.setTicket(testTicket);
        testListing.setEvent(testEvent);
        testListing.setListedPrice(new BigDecimal("75.00"));
        testListing.setComputedPrice(new BigDecimal("75.00"));
        testListing.setPriceMultiplier(BigDecimal.ONE);
        testListing.setStatus("ACTIVE");
        testListing.setListedAt(Instant.now());
    }

    @Test
    @DisplayName("getActiveListingsByEventSlug - given active listings - returns listing DTOs")
    void getActiveListingsByEventSlug_givenActiveListings_returnsListingDtos() {
        when(eventRepository.findBySlug("test-event")).thenReturn(Optional.of(testEvent));
        when(listingRepository.findByEventIdAndStatus(1L, "ACTIVE"))
                .thenReturn(List.of(testListing));

        List<ListingDto> result = listingService.getActiveListingsByEventSlug("test-event");

        assertNotNull(result, "Result should not be null");
        assertEquals(1, result.size(), "Should return one listing");
        assertEquals("Floor", result.get(0).sectionName(), "Section name should match");
    }

    @Test
    @DisplayName("getActiveListingsByEventSlug - given unknown event slug - throws ResourceNotFoundException")
    void getActiveListingsByEventSlug_givenUnknownSlug_throwsResourceNotFoundException() {
        when(eventRepository.findBySlug("nonexistent")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> listingService.getActiveListingsByEventSlug("nonexistent"),
                "Should throw ResourceNotFoundException for unknown event slug");
    }

    @Test
    @DisplayName("createListing - given available ticket - creates listing and updates ticket status")
    void createListing_givenAvailableTicket_createsListing() {
        ListingCreateRequest request = new ListingCreateRequest(
                1L, new BigDecimal("100.00"),
                Instant.now().plus(7, ChronoUnit.DAYS));

        when(ticketRepository.findById(1L)).thenReturn(Optional.of(testTicket));
        when(ticketRepository.save(any(Ticket.class))).thenReturn(testTicket);
        when(listingRepository.save(any(Listing.class))).thenAnswer(invocation -> {
            Listing listing = invocation.getArgument(0);
            listing.setId(2L);
            return listing;
        });

        ListingDto result = listingService.createListing(request);

        assertNotNull(result, "Created listing should not be null");
        verify(ticketRepository).save(any(Ticket.class));
        verify(listingRepository).save(any(Listing.class));
    }

    @Test
    @DisplayName("createListing - given unavailable ticket - throws ConflictException")
    void createListing_givenUnavailableTicket_throwsConflictException() {
        testTicket.setStatus("SOLD");
        ListingCreateRequest request = new ListingCreateRequest(
                1L, new BigDecimal("100.00"), null);

        when(ticketRepository.findById(1L)).thenReturn(Optional.of(testTicket));

        assertThrows(ConflictException.class,
                () -> listingService.createListing(request),
                "Should throw ConflictException for unavailable ticket");
    }

    @Test
    @DisplayName("updateListingPrices - given active listings - updates computed prices")
    void updateListingPrices_givenActiveListings_updatesComputedPrices() {
        BigDecimal multiplier = new BigDecimal("1.5");
        when(listingRepository.findByEventIdAndStatus(1L, "ACTIVE"))
                .thenReturn(List.of(testListing));

        listingService.updateListingPrices(1L, multiplier);

        verify(listingRepository).saveAll(any());
    }

    @Test
    @DisplayName("getActiveListingsByEventId - given active listings - returns listing DTOs")
    void getActiveListingsByEventId_givenActiveListings_returnsListingDtos() {
        when(listingRepository.findByEventIdAndStatus(1L, "ACTIVE"))
                .thenReturn(List.of(testListing));

        List<ListingDto> result = listingService.getActiveListingsByEventId(1L);

        assertEquals(1, result.size(), "Should return one listing");
        assertEquals("Floor", result.get(0).sectionName());
    }

    @Test
    @DisplayName("createListing - given unknown ticket ID - throws ResourceNotFoundException")
    void createListing_givenUnknownTicketId_throwsResourceNotFoundException() {
        ListingCreateRequest request = new ListingCreateRequest(
                99L, new BigDecimal("100.00"), null);

        when(ticketRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> listingService.createListing(request),
                "Should throw ResourceNotFoundException for unknown ticket");
    }

    @Test
    @DisplayName("toListingDto - given listing with seat and seller - maps all fields")
    void toListingDto_givenListingWithSeatAndSeller_mapsAllFields() {
        SeatRow row = new SeatRow();
        row.setId(1L);
        row.setRowLabel("B");

        Seat seat = new Seat();
        seat.setId(1L);
        seat.setSeatNumber("7");
        seat.setRow(row);

        testTicket.setSeat(seat);

        User seller = new User();
        seller.setId(1L);
        seller.setFirstName("Jane");
        seller.setLastName("Seller");
        testListing.setSeller(seller);

        when(eventRepository.findBySlug("test-event")).thenReturn(Optional.of(testEvent));
        when(listingRepository.findByEventIdAndStatus(1L, "ACTIVE"))
                .thenReturn(List.of(testListing));

        List<ListingDto> result = listingService.getActiveListingsByEventSlug("test-event");

        ListingDto dto = result.get(0);
        assertEquals("B", dto.rowLabel(), "Row label should map from seat");
        assertEquals("7", dto.seatNumber(), "Seat number should map from seat");
        assertEquals("Jane S.", dto.sellerDisplayName(),
                "Seller display name should be first name + last initial");
    }

    @Test
    @DisplayName("toListingDto - given listing without seat or seller - returns nulls")
    void toListingDto_givenListingWithoutSeatOrSeller_returnsNulls() {
        when(listingRepository.findByEventIdAndStatus(1L, "ACTIVE"))
                .thenReturn(List.of(testListing));

        List<ListingDto> result = listingService.getActiveListingsByEventId(1L);

        ListingDto dto = result.get(0);
        assertNull(dto.rowLabel(), "Row label should be null when no seat");
        assertNull(dto.seatNumber(), "Seat number should be null when no seat");
        assertNull(dto.sellerDisplayName(), "Seller should be null for platform listing");
    }
}
