package com.mockhub.ticket.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.mockhub.ticket.dto.ListingSearchCriteria;
import com.mockhub.ticket.dto.TicketComparisonResponseDto;
import com.mockhub.ticket.dto.TicketSearchResultDto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TicketComparisonServiceTest {

    @Mock
    private ListingService listingService;

    @Test
    @DisplayName("compareTickets - given no listings - returns empty comparison response")
    void compareTickets_givenNoListings_returnsEmptyComparisonResponse() {
        TicketComparisonService service = new TicketComparisonService(listingService);
        ListingSearchCriteria criteria = defaultCriteria();
        when(listingService.searchTickets(criteria)).thenReturn(List.of());

        TicketComparisonResponseDto response = service.compareTickets(criteria, null);

        assertEquals(0, response.totalMatches());
        assertTrue(response.rankedOptions().isEmpty());
        assertTrue(response.priceWarnings().isEmpty());
        assertNull(response.cheapestOption());
        assertEquals("DETERMINISTIC_HEURISTIC", response.judgmentSource());
    }

    @Test
    @DisplayName("compareTickets - given price spread - ranks best value and identifies cheapest")
    void compareTickets_givenPriceSpread_ranksBestValueAndIdentifiesCheapest() {
        TicketComparisonService service = new TicketComparisonService(listingService);
        ListingSearchCriteria criteria = defaultCriteria();
        TicketSearchResultDto cheapBalcony = searchResult(1L, "Balcony", new BigDecimal("60.00"), "STANDARD", null);
        TicketSearchResultDto floor = searchResult(2L, "Floor", new BigDecimal("90.00"), "STANDARD", null);
        TicketSearchResultDto expensiveFloor = searchResult(3L, "Floor", new BigDecimal("180.00"), "VIP", "Sam S.");
        when(listingService.searchTickets(criteria)).thenReturn(List.of(cheapBalcony, floor, expensiveFloor));

        TicketComparisonResponseDto response = service.compareTickets(criteria, "Floor");

        assertEquals(3, response.totalMatches());
        assertEquals(1L, response.cheapestOption().listing().listingId());
        assertEquals(2L, response.bestValueOption().listing().listingId());
        assertEquals(2L, response.bestSectionOption().listing().listingId());
        assertTrue(response.bestValueOption().reasonCodes().contains("MATCHES_REQUESTED_SECTION"));
        assertTrue(response.bestValueOption().score() > response.cheapestOption().score());
    }

    @Test
    @DisplayName("compareTickets - given tied prices - produces stable ranking by score then listing ID")
    void compareTickets_givenTiedPrices_producesStableRanking() {
        TicketComparisonService service = new TicketComparisonService(listingService);
        ListingSearchCriteria criteria = defaultCriteria();
        TicketSearchResultDto upper = searchResult(10L, "Upper", new BigDecimal("100.00"), "STANDARD", null);
        TicketSearchResultDto floor = searchResult(11L, "Floor", new BigDecimal("100.00"), "STANDARD", null);
        TicketSearchResultDto balcony = searchResult(12L, "Balcony", new BigDecimal("100.00"), "STANDARD", null);
        when(listingService.searchTickets(criteria)).thenReturn(List.of(upper, floor, balcony));

        TicketComparisonResponseDto response = service.compareTickets(criteria, null);

        assertEquals(11L, response.rankedOptions().get(0).listing().listingId());
        assertEquals(10L, response.rankedOptions().get(1).listing().listingId());
        assertEquals(12L, response.rankedOptions().get(2).listing().listingId());
    }

    @Test
    @DisplayName("compareTickets - given anomalous prices - surfaces price warnings")
    void compareTickets_givenAnomalousPrices_surfacesPriceWarnings() {
        TicketComparisonService service = new TicketComparisonService(listingService);
        ListingSearchCriteria criteria = defaultCriteria();
        TicketSearchResultDto veryCheap = searchResult(1L, "Floor", new BigDecimal("40.00"), "STANDARD", null);
        TicketSearchResultDto normal = searchResult(2L, "Floor", new BigDecimal("100.00"), "STANDARD", null);
        TicketSearchResultDto veryHigh = searchResult(3L, "Floor", new BigDecimal("260.00"), "STANDARD", null);
        when(listingService.searchTickets(criteria)).thenReturn(List.of(veryCheap, normal, veryHigh));

        TicketComparisonResponseDto response = service.compareTickets(criteria, null);

        assertTrue(response.priceWarnings().stream()
                .anyMatch(warning -> warning.listingId().equals(1L) && warning.code().equals("LOW_PRICE_OUTLIER")));
        assertTrue(response.priceWarnings().stream()
                .anyMatch(warning -> warning.listingId().equals(3L) && warning.code().equals("HIGH_PRICE_ANOMALY")));
        assertTrue(response.rankedOptions().stream()
                .anyMatch(option -> option.warnings().contains("HIGH_PRICE_ANOMALY")));
    }

    @Test
    @DisplayName("compareTickets - given limited market data - flags missing comparison depth")
    void compareTickets_givenLimitedMarketData_flagsMissingComparisonDepth() {
        TicketComparisonService service = new TicketComparisonService(listingService);
        ListingSearchCriteria criteria = defaultCriteria();
        TicketSearchResultDto first = searchResult(1L, "Floor", new BigDecimal("90.00"), "STANDARD", null);
        TicketSearchResultDto second = searchResult(2L, "Balcony", new BigDecimal("110.00"), "STANDARD", null);
        when(listingService.searchTickets(criteria)).thenReturn(List.of(first, second));

        TicketComparisonResponseDto response = service.compareTickets(criteria, null);

        assertEquals(2, response.totalMatches());
        assertEquals(2, response.priceWarnings().stream()
                .filter(warning -> warning.code().equals("MARKET_DATA_LIMITED"))
                .count());
    }

    @Test
    @DisplayName("compareTickets - given criteria - delegates to listing search without count query")
    void compareTickets_givenCriteria_delegatesToListingSearchWithoutCountQuery() {
        TicketComparisonService service = new TicketComparisonService(listingService);
        ListingSearchCriteria criteria = new ListingSearchCriteria(
                "rock", "concerts", "NYC", null, null, "Floor", null, null, 25);
        when(listingService.searchTickets(criteria)).thenReturn(List.of());

        service.compareTickets(criteria, "Floor");

        ArgumentCaptor<ListingSearchCriteria> captor = ArgumentCaptor.forClass(ListingSearchCriteria.class);
        verify(listingService).searchTickets(captor.capture());
        assertEquals(25, captor.getValue().limit());
    }

    private ListingSearchCriteria defaultCriteria() {
        return new ListingSearchCriteria(null, null, null, null, null, null, null, null, 10);
    }

    private TicketSearchResultDto searchResult(Long listingId, String section, BigDecimal price,
                                               String ticketType, String sellerDisplayName) {
        return new TicketSearchResultDto(
                listingId,
                listingId,
                "Rock Show",
                "rock-show",
                "Artist",
                "Concerts",
                "Venue",
                "New York",
                Instant.parse("2026-06-01T00:00:00Z"),
                section,
                "A",
                String.valueOf(listingId),
                ticketType,
                price,
                sellerDisplayName,
                "/api/v1/commerce/policies/events/rock-show"
        );
    }
}
