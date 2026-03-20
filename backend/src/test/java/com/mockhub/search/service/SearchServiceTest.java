package com.mockhub.search.service;

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

import com.mockhub.common.dto.PagedResponse;
import com.mockhub.event.entity.Category;
import com.mockhub.event.entity.Event;
import com.mockhub.event.repository.EventRepository;
import com.mockhub.search.dto.SearchResultDto;
import com.mockhub.venue.entity.Venue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SearchServiceTest {

    @Mock
    private EventRepository eventRepository;

    @InjectMocks
    private SearchService searchService;

    private Event testEvent1;
    private Event testEvent2;
    private Venue testVenue;
    private Category testCategory;

    @BeforeEach
    void setUp() {
        testVenue = new Venue();
        testVenue.setId(1L);
        testVenue.setName("Madison Square Garden");
        testVenue.setCity("New York");

        testCategory = new Category();
        testCategory.setId(1L);
        testCategory.setName("Concert");

        testEvent1 = new Event();
        testEvent1.setId(1L);
        testEvent1.setName("Taylor Swift Eras Tour");
        testEvent1.setSlug("taylor-swift-eras-tour");
        testEvent1.setArtistName("Taylor Swift");
        testEvent1.setVenue(testVenue);
        testEvent1.setCategory(testCategory);
        testEvent1.setEventDate(Instant.parse("2026-06-15T20:00:00Z"));
        testEvent1.setMinPrice(new BigDecimal("150.00"));
        testEvent1.setAvailableTickets(500);
        testEvent1.setFeatured(true);

        testEvent2 = new Event();
        testEvent2.setId(2L);
        testEvent2.setName("Beyonce Renaissance Tour");
        testEvent2.setSlug("beyonce-renaissance-tour");
        testEvent2.setArtistName("Beyonce");
        testEvent2.setVenue(testVenue);
        testEvent2.setCategory(testCategory);
        testEvent2.setEventDate(Instant.parse("2026-07-20T19:00:00Z"));
        testEvent2.setMinPrice(new BigDecimal("200.00"));
        testEvent2.setAvailableTickets(300);
        testEvent2.setFeatured(false);
    }

    @Test
    @DisplayName("search - given valid query - returns matching results")
    void search_givenValidQuery_returnsMatchingResults() {
        String query = "Taylor Swift";
        int page = 0;
        int size = 10;

        when(eventRepository.fullTextSearch(query, size, 0)).thenReturn(List.of(testEvent1));
        when(eventRepository.countFullTextSearch(query)).thenReturn(1L);

        PagedResponse<SearchResultDto> result = searchService.search(query, page, size);

        assertNotNull(result, "Search result should not be null");
        assertEquals(1, result.content().size(), "Should return one matching event");
        assertEquals("Taylor Swift Eras Tour", result.content().get(0).event().name(),
                "Should return correct event name");
        assertEquals(1.0, result.content().get(0).relevanceScore(),
                "Relevance score should be 1.0");
        assertEquals(0, result.page(), "Page number should be 0");
        assertEquals(10, result.size(), "Page size should be 10");
        assertEquals(1L, result.totalElements(), "Total elements should be 1");
        assertEquals(1, result.totalPages(), "Total pages should be 1");
    }

    @Test
    @DisplayName("search - given null query - returns empty response")
    void search_givenNullQuery_returnsEmptyResponse() {
        PagedResponse<SearchResultDto> result = searchService.search(null, 0, 10);

        assertNotNull(result, "Result should not be null");
        assertTrue(result.content().isEmpty(), "Content should be empty for null query");
        assertEquals(0, result.totalElements(), "Total elements should be 0");
        assertEquals(0, result.totalPages(), "Total pages should be 0");
        verifyNoInteractions(eventRepository);
    }

    @Test
    @DisplayName("search - given empty query - returns empty response")
    void search_givenEmptyQuery_returnsEmptyResponse() {
        PagedResponse<SearchResultDto> result = searchService.search("", 0, 10);

        assertNotNull(result, "Result should not be null");
        assertTrue(result.content().isEmpty(), "Content should be empty for empty query");
        assertEquals(0, result.totalElements(), "Total elements should be 0");
        verifyNoInteractions(eventRepository);
    }

    @Test
    @DisplayName("search - given blank query - returns empty response")
    void search_givenBlankQuery_returnsEmptyResponse() {
        PagedResponse<SearchResultDto> result = searchService.search("   ", 0, 10);

        assertNotNull(result, "Result should not be null");
        assertTrue(result.content().isEmpty(), "Content should be empty for blank query");
        verifyNoInteractions(eventRepository);
    }

    @Test
    @DisplayName("search - given query with multiple results - returns all matches")
    void search_givenQueryWithMultipleResults_returnsAllMatches() {
        String query = "concert";
        int page = 0;
        int size = 10;

        when(eventRepository.fullTextSearch(query, size, 0))
                .thenReturn(List.of(testEvent1, testEvent2));
        when(eventRepository.countFullTextSearch(query)).thenReturn(2L);

        PagedResponse<SearchResultDto> result = searchService.search(query, page, size);

        assertEquals(2, result.content().size(), "Should return two matching events");
        assertEquals(2L, result.totalElements(), "Total elements should be 2");
        assertEquals(1, result.totalPages(), "Total pages should be 1");
    }

    @Test
    @DisplayName("search - given pagination offset - passes correct offset to repository")
    void search_givenPaginationOffset_passesCorrectOffsetToRepository() {
        String query = "music";
        int page = 2;
        int size = 5;
        int expectedOffset = 10;

        when(eventRepository.fullTextSearch(query, size, expectedOffset))
                .thenReturn(List.of(testEvent1));
        when(eventRepository.countFullTextSearch(query)).thenReturn(11L);

        PagedResponse<SearchResultDto> result = searchService.search(query, page, size);

        verify(eventRepository).fullTextSearch(query, size, expectedOffset);
        assertEquals(2, result.page(), "Page number should be 2");
        assertEquals(5, result.size(), "Page size should be 5");
        assertEquals(11L, result.totalElements(), "Total elements should be 11");
        assertEquals(3, result.totalPages(), "Total pages should be 3 (ceil(11/5))");
    }

    @Test
    @DisplayName("search - given query with no results - returns empty content with zero totals")
    void search_givenQueryWithNoResults_returnsEmptyContentWithZeroTotals() {
        String query = "nonexistent";

        when(eventRepository.fullTextSearch(query, 10, 0)).thenReturn(List.of());
        when(eventRepository.countFullTextSearch(query)).thenReturn(0L);

        PagedResponse<SearchResultDto> result = searchService.search(query, 0, 10);

        assertTrue(result.content().isEmpty(), "Content should be empty");
        assertEquals(0L, result.totalElements(), "Total elements should be 0");
        assertEquals(0, result.totalPages(), "Total pages should be 0");
    }

    @Test
    @DisplayName("search - given valid query - maps event fields correctly to DTO")
    void search_givenValidQuery_mapsEventFieldsCorrectlyToDto() {
        String query = "Taylor";

        when(eventRepository.fullTextSearch(query, 10, 0)).thenReturn(List.of(testEvent1));
        when(eventRepository.countFullTextSearch(query)).thenReturn(1L);

        PagedResponse<SearchResultDto> result = searchService.search(query, 0, 10);

        SearchResultDto searchResult = result.content().get(0);
        assertEquals(1L, searchResult.event().id(), "Event ID should match");
        assertEquals("Taylor Swift Eras Tour", searchResult.event().name(), "Name should match");
        assertEquals("taylor-swift-eras-tour", searchResult.event().slug(), "Slug should match");
        assertEquals("Taylor Swift", searchResult.event().artistName(), "Artist name should match");
        assertEquals("Madison Square Garden", searchResult.event().venueName(), "Venue name should match");
        assertEquals("New York", searchResult.event().city(), "City should match");
        assertEquals(new BigDecimal("150.00"), searchResult.event().minPrice(), "Min price should match");
        assertEquals(500, searchResult.event().availableTickets(), "Available tickets should match");
        assertEquals("Concert", searchResult.event().categoryName(), "Category name should match");
        assertTrue(searchResult.event().isFeatured(), "Featured flag should match");
    }

    @Test
    @DisplayName("suggest - given valid query - returns suggestions from repository")
    void suggest_givenValidQuery_returnsSuggestionsFromRepository() {
        String query = "tay";
        List<String> expectedSuggestions = List.of("Taylor Swift Eras Tour");

        when(eventRepository.suggestEvents(query)).thenReturn(expectedSuggestions);

        List<String> result = searchService.suggest(query);

        assertEquals(expectedSuggestions, result, "Should return suggestions from repository");
        verify(eventRepository).suggestEvents(query);
    }

    @Test
    @DisplayName("suggest - given null query - returns empty list")
    void suggest_givenNullQuery_returnsEmptyList() {
        List<String> result = searchService.suggest(null);

        assertTrue(result.isEmpty(), "Should return empty list for null query");
        verifyNoInteractions(eventRepository);
    }

    @Test
    @DisplayName("suggest - given empty query - returns empty list")
    void suggest_givenEmptyQuery_returnsEmptyList() {
        List<String> result = searchService.suggest("");

        assertTrue(result.isEmpty(), "Should return empty list for empty query");
        verifyNoInteractions(eventRepository);
    }

    @Test
    @DisplayName("suggest - given blank query - returns empty list")
    void suggest_givenBlankQuery_returnsEmptyList() {
        List<String> result = searchService.suggest("   ");

        assertTrue(result.isEmpty(), "Should return empty list for blank query");
        verifyNoInteractions(eventRepository);
    }
}
