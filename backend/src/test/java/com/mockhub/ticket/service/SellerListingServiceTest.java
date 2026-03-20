package com.mockhub.ticket.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.mockhub.auth.entity.User;
import com.mockhub.auth.repository.UserRepository;
import com.mockhub.common.exception.ConflictException;
import com.mockhub.common.exception.ResourceNotFoundException;
import com.mockhub.common.exception.UnauthorizedException;
import com.mockhub.event.entity.Event;
import com.mockhub.event.repository.EventRepository;
import com.mockhub.order.entity.Order;
import com.mockhub.order.entity.OrderItem;
import com.mockhub.order.repository.OrderItemRepository;
import com.mockhub.ticket.dto.EarningsSummaryDto;
import com.mockhub.ticket.dto.SellListingRequest;
import com.mockhub.ticket.dto.SellerListingDto;
import com.mockhub.ticket.dto.UpdatePriceRequest;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SellerListingServiceTest {

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

    private User testSeller;
    private User otherUser;
    private Event testEvent;
    private Venue testVenue;
    private Section testSection;
    private SeatRow testRow;
    private Seat testSeat;
    private Ticket testTicket;
    private Listing testListing;

    @BeforeEach
    void setUp() {
        testSeller = new User();
        testSeller.setId(1L);
        testSeller.setEmail("seller@example.com");
        testSeller.setFirstName("Jane");
        testSeller.setLastName("Seller");

        otherUser = new User();
        otherUser.setId(2L);
        otherUser.setEmail("other@example.com");

        testVenue = new Venue();
        testVenue.setId(1L);
        testVenue.setName("Madison Square Garden");

        testEvent = new Event();
        testEvent.setId(1L);
        testEvent.setName("Test Concert");
        testEvent.setSlug("test-concert");
        testEvent.setEventDate(Instant.parse("2026-06-15T20:00:00Z"));
        testEvent.setVenue(testVenue);

        testSection = new Section();
        testSection.setId(1L);
        testSection.setName("Floor");

        testRow = new SeatRow();
        testRow.setId(1L);
        testRow.setRowLabel("A");
        testRow.setSection(testSection);

        testSeat = new Seat();
        testSeat.setId(1L);
        testSeat.setSeatNumber("1");
        testSeat.setRow(testRow);

        testTicket = new Ticket();
        testTicket.setId(1L);
        testTicket.setEvent(testEvent);
        testTicket.setSection(testSection);
        testTicket.setSeat(testSeat);
        testTicket.setTicketType("RESERVED");
        testTicket.setFaceValue(new BigDecimal("50.00"));
        testTicket.setStatus("AVAILABLE");

        testListing = new Listing();
        testListing.setId(1L);
        testListing.setTicket(testTicket);
        testListing.setEvent(testEvent);
        testListing.setSeller(testSeller);
        testListing.setListedPrice(new BigDecimal("75.00"));
        testListing.setComputedPrice(new BigDecimal("75.00"));
        testListing.setPriceMultiplier(BigDecimal.ONE);
        testListing.setStatus("ACTIVE");
        testListing.setListedAt(Instant.now());
        testListing.setCreatedAt(Instant.now());
    }

    // -- createSellerListing tests --

    @Test
    @DisplayName("createSellerListing - given valid request - creates listing with seller")
    void createSellerListing_givenValidRequest_createsListingWithSeller() {
        SellListingRequest request = new SellListingRequest(
                "test-concert", "Floor", "A", "1", new BigDecimal("75.00"));

        when(userRepository.findByEmail("seller@example.com"))
                .thenReturn(Optional.of(testSeller));
        when(eventRepository.findBySlug("test-concert"))
                .thenReturn(Optional.of(testEvent));
        when(ticketRepository.findByEventIdAndSectionAndRowAndSeat(
                1L, "Floor", "A", "1"))
                .thenReturn(Optional.of(testTicket));
        when(listingRepository.existsByTicketIdAndStatus(1L, "ACTIVE"))
                .thenReturn(false);
        when(ticketRepository.save(any(Ticket.class))).thenReturn(testTicket);
        when(listingRepository.save(any(Listing.class))).thenAnswer(invocation -> {
            Listing listing = invocation.getArgument(0);
            listing.setId(2L);
            listing.setCreatedAt(Instant.now());
            return listing;
        });

        SellerListingDto result = listingService.createSellerListing(
                "seller@example.com", request);

        assertNotNull(result, "Created listing should not be null");
        assertEquals("test-concert", result.eventSlug());
        assertEquals("Floor", result.sectionName());
        verify(ticketRepository).save(any(Ticket.class));
        verify(listingRepository).save(any(Listing.class));
    }

    @Test
    @DisplayName("createSellerListing - given unknown event - throws ResourceNotFoundException")
    void createSellerListing_givenUnknownEvent_throwsResourceNotFoundException() {
        SellListingRequest request = new SellListingRequest(
                "nonexistent", "Floor", "A", "1", new BigDecimal("75.00"));

        when(userRepository.findByEmail("seller@example.com"))
                .thenReturn(Optional.of(testSeller));
        when(eventRepository.findBySlug("nonexistent"))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> listingService.createSellerListing(
                        "seller@example.com", request),
                "Should throw ResourceNotFoundException for unknown event");
    }

    @Test
    @DisplayName("createSellerListing - given unavailable ticket - throws ConflictException")
    void createSellerListing_givenUnavailableTicket_throwsConflictException() {
        testTicket.setStatus("SOLD");
        SellListingRequest request = new SellListingRequest(
                "test-concert", "Floor", "A", "1", new BigDecimal("75.00"));

        when(userRepository.findByEmail("seller@example.com"))
                .thenReturn(Optional.of(testSeller));
        when(eventRepository.findBySlug("test-concert"))
                .thenReturn(Optional.of(testEvent));
        when(ticketRepository.findByEventIdAndSectionAndRowAndSeat(
                1L, "Floor", "A", "1"))
                .thenReturn(Optional.of(testTicket));

        assertThrows(ConflictException.class,
                () -> listingService.createSellerListing(
                        "seller@example.com", request),
                "Should throw ConflictException for unavailable ticket");
    }

    @Test
    @DisplayName("createSellerListing - given duplicate active listing - throws ConflictException")
    void createSellerListing_givenDuplicateActiveListing_throwsConflictException() {
        SellListingRequest request = new SellListingRequest(
                "test-concert", "Floor", "A", "1", new BigDecimal("75.00"));

        when(userRepository.findByEmail("seller@example.com"))
                .thenReturn(Optional.of(testSeller));
        when(eventRepository.findBySlug("test-concert"))
                .thenReturn(Optional.of(testEvent));
        when(ticketRepository.findByEventIdAndSectionAndRowAndSeat(
                1L, "Floor", "A", "1"))
                .thenReturn(Optional.of(testTicket));
        when(listingRepository.existsByTicketIdAndStatus(1L, "ACTIVE"))
                .thenReturn(true);

        assertThrows(ConflictException.class,
                () -> listingService.createSellerListing(
                        "seller@example.com", request),
                "Should throw ConflictException for duplicate listing");
    }

    @Test
    @DisplayName("createSellerListing - given unknown seat - throws ResourceNotFoundException")
    void createSellerListing_givenUnknownSeat_throwsResourceNotFoundException() {
        SellListingRequest request = new SellListingRequest(
                "test-concert", "Balcony", "Z", "99", new BigDecimal("75.00"));

        when(userRepository.findByEmail("seller@example.com"))
                .thenReturn(Optional.of(testSeller));
        when(eventRepository.findBySlug("test-concert"))
                .thenReturn(Optional.of(testEvent));
        when(ticketRepository.findByEventIdAndSectionAndRowAndSeat(
                1L, "Balcony", "Z", "99"))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> listingService.createSellerListing(
                        "seller@example.com", request),
                "Should throw ResourceNotFoundException for unknown seat");
    }

    @Test
    @DisplayName("updateListingPrice - given unknown listing ID - throws ResourceNotFoundException")
    void updateListingPrice_givenUnknownListingId_throwsResourceNotFoundException() {
        UpdatePriceRequest request = new UpdatePriceRequest(
                new BigDecimal("100.00"));

        when(userRepository.findByEmail("seller@example.com"))
                .thenReturn(Optional.of(testSeller));
        when(listingRepository.findById(99L))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> listingService.updateListingPrice(
                        99L, "seller@example.com", request),
                "Should throw ResourceNotFoundException for unknown listing");
    }

    @Test
    @DisplayName("resolveUser - given unknown email - throws ResourceNotFoundException")
    void getSellerListings_givenUnknownUser_throwsResourceNotFoundException() {
        when(userRepository.findByEmail("nobody@example.com"))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> listingService.getSellerListings("nobody@example.com", null),
                "Should throw ResourceNotFoundException for unknown user");
    }

    // -- getSellerListings tests --

    @Test
    @DisplayName("getSellerListings - given no status filter - returns all listings")
    void getSellerListings_givenNoStatusFilter_returnsAllListings() {
        when(userRepository.findByEmail("seller@example.com"))
                .thenReturn(Optional.of(testSeller));
        when(listingRepository.findBySellerId(1L))
                .thenReturn(List.of(testListing));

        List<SellerListingDto> result = listingService.getSellerListings(
                "seller@example.com", null);

        assertEquals(1, result.size(), "Should return one listing");
        assertEquals("Test Concert", result.get(0).eventName());
    }

    @Test
    @DisplayName("getSellerListings - given ACTIVE status filter - returns only active")
    void getSellerListings_givenActiveFilter_returnsOnlyActive() {
        when(userRepository.findByEmail("seller@example.com"))
                .thenReturn(Optional.of(testSeller));
        when(listingRepository.findBySellerIdAndStatus(1L, "ACTIVE"))
                .thenReturn(List.of(testListing));

        List<SellerListingDto> result = listingService.getSellerListings(
                "seller@example.com", "ACTIVE");

        assertEquals(1, result.size(), "Should return one active listing");
    }

    // -- updateListingPrice tests --

    @Test
    @DisplayName("updateListingPrice - given valid owner and price - updates price")
    void updateListingPrice_givenValidOwnerAndPrice_updatesPrice() {
        UpdatePriceRequest request = new UpdatePriceRequest(
                new BigDecimal("100.00"));

        when(userRepository.findByEmail("seller@example.com"))
                .thenReturn(Optional.of(testSeller));
        when(listingRepository.findById(1L))
                .thenReturn(Optional.of(testListing));
        when(listingRepository.save(any(Listing.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        SellerListingDto result = listingService.updateListingPrice(
                1L, "seller@example.com", request);

        assertEquals(new BigDecimal("100.00"), result.listedPrice(),
                "Listed price should be updated");
    }

    @Test
    @DisplayName("updateListingPrice - given non-owner - throws UnauthorizedException")
    void updateListingPrice_givenNonOwner_throwsUnauthorizedException() {
        UpdatePriceRequest request = new UpdatePriceRequest(
                new BigDecimal("100.00"));

        when(userRepository.findByEmail("other@example.com"))
                .thenReturn(Optional.of(otherUser));
        when(listingRepository.findById(1L))
                .thenReturn(Optional.of(testListing));

        assertThrows(UnauthorizedException.class,
                () -> listingService.updateListingPrice(
                        1L, "other@example.com", request),
                "Should throw UnauthorizedException for non-owner");
    }

    @Test
    @DisplayName("updateListingPrice - given sold listing - throws ConflictException")
    void updateListingPrice_givenSoldListing_throwsConflictException() {
        testListing.setStatus("SOLD");
        UpdatePriceRequest request = new UpdatePriceRequest(
                new BigDecimal("100.00"));

        when(userRepository.findByEmail("seller@example.com"))
                .thenReturn(Optional.of(testSeller));
        when(listingRepository.findById(1L))
                .thenReturn(Optional.of(testListing));

        assertThrows(ConflictException.class,
                () -> listingService.updateListingPrice(
                        1L, "seller@example.com", request),
                "Should throw ConflictException for sold listing");
    }

    // -- deactivateListing tests --

    @Test
    @DisplayName("deactivateListing - given valid owner - cancels listing and frees ticket")
    void deactivateListing_givenValidOwner_cancelsListingAndFreesTicket() {
        when(userRepository.findByEmail("seller@example.com"))
                .thenReturn(Optional.of(testSeller));
        when(listingRepository.findById(1L))
                .thenReturn(Optional.of(testListing));

        listingService.deactivateListing(1L, "seller@example.com");

        assertEquals("CANCELLED", testListing.getStatus(),
                "Listing status should be CANCELLED");
        assertEquals("AVAILABLE", testTicket.getStatus(),
                "Ticket status should be AVAILABLE");
        verify(listingRepository).save(testListing);
        verify(ticketRepository).save(testTicket);
    }

    @Test
    @DisplayName("deactivateListing - given non-owner - throws UnauthorizedException")
    void deactivateListing_givenNonOwner_throwsUnauthorizedException() {
        when(userRepository.findByEmail("other@example.com"))
                .thenReturn(Optional.of(otherUser));
        when(listingRepository.findById(1L))
                .thenReturn(Optional.of(testListing));

        assertThrows(UnauthorizedException.class,
                () -> listingService.deactivateListing(
                        1L, "other@example.com"),
                "Should throw UnauthorizedException for non-owner");
    }

    @Test
    @DisplayName("deactivateListing - given listing not found - throws ResourceNotFoundException")
    void deactivateListing_givenListingNotFound_throwsResourceNotFoundException() {
        when(userRepository.findByEmail("seller@example.com"))
                .thenReturn(Optional.of(testSeller));
        when(listingRepository.findById(99L))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> listingService.deactivateListing(
                        99L, "seller@example.com"),
                "Should throw ResourceNotFoundException for missing listing");
    }

    @Test
    @DisplayName("deactivateListing - given sold listing - throws ConflictException")
    void deactivateListing_givenSoldListing_throwsConflictException() {
        testListing.setStatus("SOLD");
        when(userRepository.findByEmail("seller@example.com"))
                .thenReturn(Optional.of(testSeller));
        when(listingRepository.findById(1L))
                .thenReturn(Optional.of(testListing));

        assertThrows(ConflictException.class,
                () -> listingService.deactivateListing(
                        1L, "seller@example.com"),
                "Should throw ConflictException for sold listing");
    }

    // -- getEarningsSummary tests --

    @Test
    @DisplayName("getEarningsSummary - given seller with sales - returns summary")
    void getEarningsSummary_givenSellerWithSales_returnsSummary() {
        when(userRepository.findByEmail("seller@example.com"))
                .thenReturn(Optional.of(testSeller));
        when(orderItemRepository.sumEarningsBySellerId(1L))
                .thenReturn(new BigDecimal("150.00"));
        when(listingRepository.countBySellerIdAndStatus(1L, "ACTIVE"))
                .thenReturn(2L);
        when(listingRepository.countBySellerIdAndStatus(1L, "SOLD"))
                .thenReturn(3L);
        when(listingRepository.countBySellerIdAndStatus(1L, "CANCELLED"))
                .thenReturn(1L);

        Order completedOrder = new Order();
        completedOrder.setId(1L);
        completedOrder.setOrderNumber("MH-20260320-0001");
        completedOrder.setStatus("COMPLETED");
        completedOrder.setConfirmedAt(Instant.now());

        OrderItem orderItem = new OrderItem();
        orderItem.setId(1L);
        orderItem.setOrder(completedOrder);
        orderItem.setListing(testListing);
        orderItem.setTicket(testTicket);
        orderItem.setPricePaid(new BigDecimal("75.00"));

        when(orderItemRepository.findCompletedSalesBySellerId(1L))
                .thenReturn(List.of(orderItem));

        EarningsSummaryDto result = listingService.getEarningsSummary(
                "seller@example.com");

        assertNotNull(result, "Earnings summary should not be null");
        assertEquals(new BigDecimal("150.00"), result.totalEarnings());
        assertEquals(6L, result.totalListings(), "Total should be 2+3+1=6");
        assertEquals(2L, result.activeListings());
        assertEquals(3L, result.soldListings());
        assertEquals(1, result.recentSales().size());
    }

    @Test
    @DisplayName("getEarningsSummary - given seller with no sales - returns zero earnings")
    void getEarningsSummary_givenSellerWithNoSales_returnsZeroEarnings() {
        when(userRepository.findByEmail("seller@example.com"))
                .thenReturn(Optional.of(testSeller));
        when(orderItemRepository.sumEarningsBySellerId(1L))
                .thenReturn(BigDecimal.ZERO);
        when(listingRepository.countBySellerIdAndStatus(eq(1L), any()))
                .thenReturn(0L);
        when(orderItemRepository.findCompletedSalesBySellerId(1L))
                .thenReturn(List.of());

        EarningsSummaryDto result = listingService.getEarningsSummary(
                "seller@example.com");

        assertEquals(BigDecimal.ZERO, result.totalEarnings());
        assertEquals(0L, result.totalListings());
        assertEquals(0, result.recentSales().size());
    }
}
