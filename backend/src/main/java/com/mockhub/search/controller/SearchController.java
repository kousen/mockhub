package com.mockhub.search.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.mockhub.common.dto.PagedResponse;
import com.mockhub.search.dto.SearchResultDto;
import com.mockhub.search.service.SearchService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/search")
@Tag(name = "Search", description = "Full-text search and autocomplete")
public class SearchController {

    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    @GetMapping
    @Operation(summary = "Search events", description = "Full-text search across event names, artists, and descriptions")
    @ApiResponse(responseCode = "200", description = "Search results returned")
    public ResponseEntity<PagedResponse<SearchResultDto>> search(
            @Parameter(description = "Search query", example = "Taylor Swift")
            @RequestParam String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(searchService.search(q, page, size));
    }

    @GetMapping("/suggest")
    @Operation(summary = "Autocomplete suggestions", description = "Return up to 10 event name suggestions matching the query")
    @ApiResponse(responseCode = "200", description = "Suggestions returned")
    public ResponseEntity<List<String>> suggest(
            @Parameter(description = "Partial search query", example = "Tay")
            @RequestParam String q) {
        return ResponseEntity.ok(searchService.suggest(q));
    }
}
