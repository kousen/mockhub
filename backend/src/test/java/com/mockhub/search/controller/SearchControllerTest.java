package com.mockhub.search.controller;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.mockhub.auth.security.JwtAuthenticationFilter;
import com.mockhub.auth.security.JwtTokenProvider;
import com.mockhub.auth.security.UserDetailsServiceImpl;
import com.mockhub.common.dto.PagedResponse;
import com.mockhub.config.SecurityConfig;
import com.mockhub.event.dto.EventSummaryDto;
import com.mockhub.search.dto.SearchResultDto;
import com.mockhub.search.service.SearchService;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SearchController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class SearchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SearchService searchService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private UserDetailsServiceImpl userDetailsService;

    private EventSummaryDto createTestEventSummary(String name, String slug) {
        return new EventSummaryDto(
                1L, name, slug, "The Artist",
                "Madison Square Garden", "New York",
                Instant.now().plus(30, ChronoUnit.DAYS),
                new BigDecimal("75.00"), 500, null, "Concert", true);
    }

    private SearchResultDto createTestSearchResult(String name, String slug, double score) {
        return new SearchResultDto(createTestEventSummary(name, slug), score);
    }

    @Test
    @DisplayName("GET /api/v1/search - no auth required - returns search results")
    void search_noAuthRequired_returnsSearchResults() throws Exception {
        PagedResponse<SearchResultDto> response = new PagedResponse<>(
                List.of(
                        createTestSearchResult("Taylor Swift Concert", "taylor-swift-concert", 0.95),
                        createTestSearchResult("Taylor Hawkins Tribute", "taylor-hawkins-tribute", 0.72)),
                0, 20, 2, 1);
        when(searchService.search("Taylor", 0, 20)).thenReturn(response);

        mockMvc.perform(get("/api/v1/search")
                        .param("q", "Taylor"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].event.name").value("Taylor Swift Concert"))
                .andExpect(jsonPath("$.content[0].relevanceScore").value(0.95))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    @DisplayName("GET /api/v1/search - with pagination params - passes params to service")
    void search_withPaginationParams_passesParamsToService() throws Exception {
        PagedResponse<SearchResultDto> response = new PagedResponse<>(
                List.of(), 1, 10, 0, 0);
        when(searchService.search("rock", 1, 10)).thenReturn(response);

        mockMvc.perform(get("/api/v1/search")
                        .param("q", "rock")
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(0));
    }

    @Test
    @DisplayName("GET /api/v1/search - missing query param - returns error")
    void search_missingQueryParam_returnsError() throws Exception {
        mockMvc.perform(get("/api/v1/search"))
                .andExpect(status().is5xxServerError());
    }

    @Test
    @DisplayName("GET /api/v1/search - empty results - returns empty page")
    void search_emptyResults_returnsEmptyPage() throws Exception {
        PagedResponse<SearchResultDto> response = new PagedResponse<>(
                List.of(), 0, 20, 0, 0);
        when(searchService.search("xyznonexistent", 0, 20)).thenReturn(response);

        mockMvc.perform(get("/api/v1/search")
                        .param("q", "xyznonexistent"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(0))
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    @DisplayName("GET /api/v1/search/suggest - no auth required - returns suggestions")
    void suggest_noAuthRequired_returnsSuggestions() throws Exception {
        when(searchService.suggest("Tay"))
                .thenReturn(List.of("Taylor Swift Concert", "Taylor Hawkins Tribute"));

        mockMvc.perform(get("/api/v1/search/suggest")
                        .param("q", "Tay"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0]").value("Taylor Swift Concert"))
                .andExpect(jsonPath("$[1]").value("Taylor Hawkins Tribute"));
    }

    @Test
    @DisplayName("GET /api/v1/search/suggest - missing query param - returns error")
    void suggest_missingQueryParam_returnsError() throws Exception {
        mockMvc.perform(get("/api/v1/search/suggest"))
                .andExpect(status().is5xxServerError());
    }

    @Test
    @DisplayName("GET /api/v1/search/suggest - no matches - returns empty list")
    void suggest_noMatches_returnsEmptyList() throws Exception {
        when(searchService.suggest("zzz")).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/search/suggest")
                        .param("q", "zzz"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }
}
