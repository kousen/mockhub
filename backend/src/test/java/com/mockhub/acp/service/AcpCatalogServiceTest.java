package com.mockhub.acp.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.mockhub.acp.dto.AcpCatalogItem;
import com.mockhub.acp.dto.AcpListingItem;
import com.mockhub.common.dto.PagedResponse;
import com.mockhub.event.dto.EventSearchRequest;
import com.mockhub.event.dto.EventSummaryDto;
import com.mockhub.event.service.EventService;
import com.mockhub.ticket.dto.ListingSearchCriteria;
import com.mockhub.ticket.entity.Listing;
import com.mockhub.ticket.repository.ListingRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AcpCatalogServiceTest {

    @Mock
    private EventService eventService;

    @Mock
    private ListingRepository listingRepository;

    @InjectMocks
    private AcpCatalogService acpCatalogService;

    private EventSummaryDto testEvent;

    @BeforeEach
    void setUp() {
        testEvent = new EventSummaryDto(
                1L, "Rock Festival", "rock-festival", "Band A",
                "Madison Square Garden", "NYC", Instant.now(),
                new BigDecimal("75.00"), 50, null, "rock", true);
    }

    @Test
    @DisplayName("getCatalog - given events exist - returns catalog items")
    void getCatalog_givenEventsExist_returnsCatalogItems() {
        PagedResponse<EventSummaryDto> eventPage = new PagedResponse<>(List.of(testEvent), 0, 20, 1, 1);
        when(eventService.listEvents(any(EventSearchRequest.class))).thenReturn(eventPage);
        PagedResponse<AcpCatalogItem> result = acpCatalogService.getCatalog("rock", "rock", "NYC", 0, 20);
        assertNotNull(result);
        assertEquals(1, result.content().size());
        AcpCatalogItem item = result.content().getFirst();
        assertEquals("rock-festival", item.productId());
        assertEquals("Rock Festival", item.name());
        assertEquals("Band A", item.description());
        assertEquals("rock", item.category());
        assertEquals("Madison Square Garden", item.venue());
        assertEquals("NYC", item.city());
        assertEquals(new BigDecimal("75.00"), item.minPrice());
        assertEquals(50, item.availableTickets());
        assertEquals("/events/rock-festival", item.url());
    }

    @Test
    @DisplayName("getCatalog - given no events - returns empty catalog")
    void getCatalog_givenNoEvents_returnsEmptyCatalog() {
        when(eventService.listEvents(any(EventSearchRequest.class))).thenReturn(new PagedResponse<>(List.of(), 0, 20, 0, 0));
        PagedResponse<AcpCatalogItem> result = acpCatalogService.getCatalog(null, null, null, 0, 20);
        assertNotNull(result);
        assertEquals(0, result.content().size());
        assertEquals(0, result.totalElements());
    }

    @Test
    @DisplayName("getCatalog - given event with null artistName - uses event name as description")
    void getCatalog_givenEventWithNullArtistName_usesEventNameAsDescription() {
        EventSummaryDto noArtistEvent = new EventSummaryDto(1L, "Sports Game", "sports-game", null,
                "Stadium", "Chicago", Instant.now(), new BigDecimal("100.00"), 200, null, "sports", false);
        when(eventService.listEvents(any(EventSearchRequest.class))).thenReturn(new PagedResponse<>(List.of(noArtistEvent), 0, 20, 1, 1));
        PagedResponse<AcpCatalogItem> result = acpCatalogService.getCatalog(null, null, null, 0, 20);
        assertNotNull(result);
        assertEquals("Sports Game", result.content().getFirst().description());
    }

    @Test
    @DisplayName("getListings - given matching events and listings - returns priced offer items")
    void getListings_givenMatchingEventsAndListings_returnsOfferItems() {
        when(eventService.listEvents(any(EventSearchRequest.class))).thenReturn(new PagedResponse<>(List.of(testEvent), 0, 20, 1, 1));
        Listing listing = createListing(10L, "Floor", new BigDecimal("80.00"));
        when(listingRepository.findByEventIdAndStatus(1L, "ACTIVE")).thenReturn(List.of(listing));
        ListingSearchCriteria criteria = new ListingSearchCriteria("rock", "rock", "NYC",
                new BigDecimal("50.00"), new BigDecimal("100.00"), null, null, null, 20);
        PagedResponse<AcpListingItem> result = acpCatalogService.getListings(criteria, 0, 20);
        assertNotNull(result);
        assertEquals(1, result.content().size());
        assertEquals(10L, result.content().getFirst().listingId());
        assertEquals(new BigDecimal("80.00"), result.content().getFirst().price());
    }

    @Test
    @DisplayName("getListings - given filters - returns filtered results")
    void getListings_givenFilters_returnsFilteredResults() {
        when(eventService.listEvents(any(EventSearchRequest.class))).thenReturn(new PagedResponse<>(List.of(testEvent), 0, 100, 1, 1));
        when(listingRepository.findByEventIdAndStatus(1L, "ACTIVE")).thenReturn(List.of(
                createListing(10L, "Floor", new BigDecimal("80.00")),
                createListing(20L, "VIP", new BigDecimal("200.00")),
                createListing(30L, "Floor", new BigDecimal("30.00"))));
        ListingSearchCriteria criteria = new ListingSearchCriteria("rock", "rock", "NYC",
                new BigDecimal("50.00"), new BigDecimal("150.00"), "Floor", null, null, 20);
        PagedResponse<AcpListingItem> result = acpCatalogService.getListings(criteria, 0, 20);
        assertNotNull(result);
        assertEquals(1, result.content().size(), "Only the $80 Floor listing should match all filters");
        assertEquals(10L, result.content().getFirst().listingId());
        assertEquals(new BigDecimal("80.00"), result.content().getFirst().price());
    }

    private Listing createListing(long listingId, String sectionName, BigDecimal price) {
        com.mockhub.event.entity.Event event = new com.mockhub.event.entity.Event();
        event.setSlug("test-concert"); event.setName("Test Concert");
        com.mockhub.event.entity.Category category = new com.mockhub.event.entity.Category();
        category.setSlug("concerts"); event.setCategory(category);
        event.setStatus("ACTIVE"); event.setEventDate(Instant.now().plusSeconds(86_400));
        Listing listing = new Listing();
        listing.setId(listingId); listing.setEvent(event); listing.setStatus("ACTIVE"); listing.setComputedPrice(price);
        com.mockhub.ticket.entity.Ticket ticket = new com.mockhub.ticket.entity.Ticket();
        com.mockhub.venue.entity.Section section = new com.mockhub.venue.entity.Section();
        section.setName(sectionName); ticket.setSection(section); listing.setTicket(ticket);
        return listing;
    }
}
