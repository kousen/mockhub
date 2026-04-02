package com.mockhub.ticketmaster.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.mockhub.ticketmaster.dto.TicketmasterEventResponse;
import com.mockhub.ticketmaster.dto.TicketmasterSearchResponse;

import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

@Service
@Profile("ticketmaster")
@Primary
public class TicketmasterApiService implements TicketmasterService {

    private static final Logger log = LoggerFactory.getLogger(TicketmasterApiService.class);
    private static final int MAX_RETRIES = 3;
    private static final long BASE_BACKOFF_MS = 1000;

    private final RestClient restClient;
    private final JsonMapper jsonMapper;
    private final String apiKey;

    @Autowired
    public TicketmasterApiService(
            @Value("${mockhub.ticketmaster.api-key}") String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                    "TICKETMASTER_API_KEY must be set when 'ticketmaster' profile is active");
        }
        this.apiKey = apiKey;
        this.jsonMapper = JsonMapper.builder()
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .build();
        this.restClient = RestClient.builder()
                .baseUrl("https://app.ticketmaster.com/discovery/v2")
                .build();
    }

    TicketmasterApiService(RestClient restClient, String apiKey) {
        this.restClient = restClient;
        this.apiKey = apiKey;
        this.jsonMapper = JsonMapper.builder()
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .build();
    }

    @Override
    public List<TicketmasterEventResponse> searchEvents(String classificationName,
                                                         String startDateTime,
                                                         String endDateTime,
                                                         int size,
                                                         int page) {
        TicketmasterSearchResponse response = fetchWithRetry(
                classificationName, startDateTime, endDateTime, size, page);

        if (response == null || response.embedded() == null
                || response.embedded().events() == null) {
            return List.of();
        }

        log.info("Fetched {} events for classification '{}' (page {})",
                response.embedded().events().size(), classificationName, page);
        return response.embedded().events();
    }

    private TicketmasterSearchResponse fetchWithRetry(String classificationName,
                                                       String startDateTime,
                                                       String endDateTime,
                                                       int size,
                                                       int page) {
        for (int attempt = 0; attempt <= MAX_RETRIES; attempt++) {
            try {
                // Fetch as String and deserialize manually with Jackson 3 JsonMapper.
                // Spring Boot 4's RestClient HTTP converter loses generic type info for
                // Map<String, List<ExternalLink>> nested 3 levels deep in _embedded records.
                String json = restClient.get()
                        .uri(uriBuilder -> uriBuilder
                                .path("/events.json")
                                .queryParam("apikey", apiKey)
                                .queryParam("classificationName", classificationName)
                                .queryParam("startDateTime", startDateTime)
                                .queryParam("endDateTime", endDateTime)
                                .queryParam("size", size)
                                .queryParam("page", page)
                                .queryParam("sort", "relevance,desc")
                                .build())
                        .retrieve()
                        .body(String.class);
                return jsonMapper.readValue(json, TicketmasterSearchResponse.class);
            } catch (HttpClientErrorException.TooManyRequests e) {
                if (attempt == MAX_RETRIES) {
                    log.error("Ticketmaster rate limit exceeded after {} retries", MAX_RETRIES);
                    throw e;
                }
                String retryAfterHeader = e.getResponseHeaders() != null
                        ? e.getResponseHeaders().getFirst("Retry-After")
                        : null;
                long waitMs = parseRetryAfter(retryAfterHeader, attempt);
                log.warn("Ticketmaster rate limited (attempt {}/{}), waiting {}ms",
                        attempt + 1, MAX_RETRIES, waitMs);
                sleep(waitMs);
            } catch (RestClientException e) {
                Throwable cause = e.getCause() != null ? e.getCause() : e;
                log.error("Ticketmaster API error: {} — cause: {}", e.getMessage(), cause.toString());
                throw e;
            }
        }
        throw new RestClientException("Ticketmaster API request failed after retries");
    }

    long parseRetryAfter(String retryAfterHeader, int attempt) {
        if (retryAfterHeader != null) {
            try {
                return Long.parseLong(retryAfterHeader) * 1000;
            } catch (NumberFormatException _) {
                // Fall through to exponential backoff
            }
        }
        return BASE_BACKOFF_MS * (1L << attempt);
    }

    void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RestClientException("Interrupted while waiting for Ticketmaster rate limit", e);
        }
    }
}
