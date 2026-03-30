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
import com.mockhub.event.entity.Category;
import com.mockhub.ticket.dto.ListingCreateRequest;
import com.mockhub.ticket.dto.ListingDto;
import com.mockhub.ticket.dto.ListingSearchCriteria;
import com.mockhub.ticket.dto.TicketSearchResultDto;
import com.mockhub.auth.entity.User;
import com.mockhub.ticket.entity.Listing;
import com.mockhub.ticket.entity.Ticket;
import com.mockhub.ticket.repository.ListingRepository;
import com.mockhub.ticket.repository.TicketRepository;
import com.mockhub.venue.entity.Seat;
import com.mockhub.venue.entity.SeatRow;
import com.mockhub.venue.entity.Section;
import com.mockhub.venue.entity.Venue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
    @DisplayName("getActiveListingsByEventSlugPaginated - given valid slug - returns paginated listings")
    void getActiveListingsByEventSlugPaginated_givenValidSlug_returnsPaginatedListings() {
        when(eventRepository.findBySlug("test-event")).thenReturn(Optional.of(testEvent));
        when(listingRepository.findByEventIdAndStatusOrderByPrice(eq(1L), eq("ACTIVE"), any()))
                .thenReturn(List.of(testListing));

        List<ListingDto> result = listingService.getActiveListingsByEventSlugPaginated("test-event", 0, 20);

        assertEquals(1, result.size());
        assertEquals("Floor", result.get(0).sectionName());
    }

    @Test
    @DisplayName("countActiveListingsByEventSlug - given valid slug - returns count")
    void countActiveListingsByEventSlug_givenValidSlug_returnsCount() {
        when(eventRepository.findBySlug("test-event")).thenReturn(Optional.of(testEvent));
        when(listingRepository.countByEventIdAndStatus(1L, "ACTIVE")).thenReturn(42L);

        long count = listingService.countActiveListingsByEventSlug("test-event");

        assertEquals(42L, count);
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

    // --- searchTickets ---

    private ListingSearchCriteria defaultCriteria() {
        return new ListingSearchCriteria(null, null, null, null, null, null, null, null, 10);
    }

    @SuppressWarnings("unchecked")
    private void stubSpecSearch(List<Listing> listings) {
        List<Long> ids = listings.stream().map(Listing::getId).toList();
        when(listingRepository.findBy(
                any(org.springframework.data.jpa.domain.Specification.class),
                any(java.util.function.Function.class)))
                .thenReturn(listings);
        if (!ids.isEmpty()) {
            when(listingRepository.findByIdsWithDetails(ids)).thenReturn(listings);
        }
    }

    @Test
    @DisplayName("searchTickets - given matching listings - returns search result DTOs")
    void searchTickets_givenMatchingListings_returnsSearchResultDtos() {
        Listing listing = createFullListing(false, false);
        stubSpecSearch(List.of(listing));

        List<TicketSearchResultDto> results = listingService.searchTickets(defaultCriteria());

        assertEquals(1, results.size());
        assertEquals("Test Event", results.get(0).eventName());
        assertEquals("test-event", results.get(0).eventSlug());
        assertEquals("Floor", results.get(0).sectionName());
        assertEquals(new BigDecimal("75.00"), results.get(0).price());
    }

    @Test
    @DisplayName("searchTickets - given listing with seat - includes row and seat info")
    void searchTickets_givenListingWithSeat_includesRowAndSeatInfo() {
        Listing listing = createFullListing(true, false);
        stubSpecSearch(List.of(listing));

        List<TicketSearchResultDto> results = listingService.searchTickets(defaultCriteria());

        assertEquals("A", results.get(0).rowLabel());
        assertEquals("1", results.get(0).seatNumber());
    }

    @Test
    @DisplayName("searchTickets - given listing with seller - includes seller display name")
    void searchTickets_givenListingWithSeller_includesSellerDisplayName() {
        Listing listing = createFullListing(false, true);
        stubSpecSearch(List.of(listing));

        List<TicketSearchResultDto> results = listingService.searchTickets(defaultCriteria());

        assertEquals("Jane D.", results.get(0).sellerDisplayName());
    }

    @Test
    @DisplayName("searchTickets - given listing without seat or seller - returns nulls for optional fields")
    void searchTickets_givenListingWithoutSeatOrSeller_returnsNullsForOptionalFields() {
        Listing listing = createFullListing(false, false);
        stubSpecSearch(List.of(listing));

        List<TicketSearchResultDto> results = listingService.searchTickets(defaultCriteria());

        assertNull(results.get(0).rowLabel());
        assertNull(results.get(0).seatNumber());
        assertNull(results.get(0).sellerDisplayName());
    }

    @SuppressWarnings("unchecked")
    @Test
    @DisplayName("searchTickets - given limit - passes limit via fluent API")
    void searchTickets_givenLimit_passesLimitToQuery() {
        when(listingRepository.findBy(
                any(org.springframework.data.jpa.domain.Specification.class),
                any(java.util.function.Function.class)))
                .thenReturn(List.of());

        ListingSearchCriteria criteria = new ListingSearchCriteria(
                null, null, null, null, null, null, null, null, 5);
        List<TicketSearchResultDto> results = listingService.searchTickets(criteria);

        assertEquals(0, results.size());
        verify(listingRepository).findBy(
                any(org.springframework.data.jpa.domain.Specification.class),
                any(java.util.function.Function.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    @DisplayName("searchTickets - given no results - returns empty list")
    void searchTickets_givenNoResults_returnsEmptyList() {
        when(listingRepository.findBy(
                any(org.springframework.data.jpa.domain.Specification.class),
                any(java.util.function.Function.class)))
                .thenReturn(List.of());

        ListingSearchCriteria criteria = new ListingSearchCriteria(
                "nonexistent", null, null, null, null, null, null, null, 10);
        List<TicketSearchResultDto> results = listingService.searchTickets(criteria);

        assertEquals(0, results.size());
    }

    // --- getComputedPriceRange ---

    @Test
    @DisplayName("getComputedPriceRange - given listings with prices - returns min and max")
    void getComputedPriceRange_givenListingsWithPrices_returnsMinAndMax() {
        Listing listing1 = new Listing();
        listing1.setComputedPrice(new BigDecimal("50.00"));
        listing1.setStatus("ACTIVE");

        Listing listing2 = new Listing();
        listing2.setComputedPrice(new BigDecimal("150.00"));
        listing2.setStatus("ACTIVE");

        Listing listing3 = new Listing();
        listing3.setComputedPrice(new BigDecimal("75.00"));
        listing3.setStatus("ACTIVE");

        when(listingRepository.findByEventIdAndStatus(1L, "ACTIVE"))
                .thenReturn(List.of(listing1, listing2, listing3));

        BigDecimal[] result = listingService.getComputedPriceRange(1L);

        assertNotNull(result, "Result should not be null");
        assertEquals(new BigDecimal("50.00"), result[0], "Min price should be 50.00");
        assertEquals(new BigDecimal("150.00"), result[1], "Max price should be 150.00");
    }

    @Test
    @DisplayName("getComputedPriceRange - given no listings - returns nulls")
    void getComputedPriceRange_givenNoListings_returnsNulls() {
        when(listingRepository.findByEventIdAndStatus(1L, "ACTIVE"))
                .thenReturn(List.of());

        BigDecimal[] result = listingService.getComputedPriceRange(1L);

        assertNotNull(result, "Result array should not be null");
        assertNull(result[0], "Min price should be null when no listings");
        assertNull(result[1], "Max price should be null when no listings");
    }

    private Listing createFullListing(boolean withSeat, boolean withSeller) {
        Venue venue = new Venue();
        venue.setName("Test Venue");
        venue.setCity("New York");

        Category category = new Category();
        category.setName("Rock");

        Event event = new Event();
        event.setId(1L);
        event.setName("Test Event");
        event.setSlug("test-event");
        event.setArtistName("Test Artist");
        event.setEventDate(Instant.now().plus(30, ChronoUnit.DAYS));
        event.setVenue(venue);
        event.setCategory(category);

        Section section = new Section();
        section.setId(1L);
        section.setName("Floor");

        Ticket ticket = new Ticket();
        ticket.setId(1L);
        ticket.setEvent(event);
        ticket.setSection(section);
        ticket.setTicketType("STANDARD");

        if (withSeat) {
            SeatRow row = new SeatRow();
            row.setRowLabel("A");
            Seat seat = new Seat();
            seat.setRow(row);
            seat.setSeatNumber("1");
            ticket.setSeat(seat);
        }

        Listing listing = new Listing();
        listing.setId(1L);
        listing.setTicket(ticket);
        listing.setEvent(event);
        listing.setComputedPrice(new BigDecimal("75.00"));
        listing.setStatus("ACTIVE");

        if (withSeller) {
            User seller = new User();
            seller.setFirstName("Jane");
            seller.setLastName("Doe");
            listing.setSeller(seller);
        }

        return listing;
    }
}
