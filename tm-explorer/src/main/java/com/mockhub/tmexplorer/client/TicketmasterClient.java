package com.mockhub.tmexplorer.client;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import com.mockhub.tmexplorer.model.EventResponse;
import com.mockhub.tmexplorer.model.SearchResponse;

import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

/**
 * Minimal Ticketmaster Discovery API client using pure Jackson 3 + java.net.http.
 * No Spring, no Jackson 2 on the classpath.
 */
public class TicketmasterClient {

    private static final String BASE_URL = "https://app.ticketmaster.com/discovery/v2";

    private final HttpClient httpClient;
    private final JsonMapper jsonMapper;
    private final String apiKey;

    public TicketmasterClient(String apiKey) {
        this.apiKey = apiKey;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.jsonMapper = JsonMapper.builder()
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .build();
    }

    // Package-private for testing with a custom HttpClient
    TicketmasterClient(String apiKey, HttpClient httpClient, JsonMapper jsonMapper) {
        this.apiKey = apiKey;
        this.httpClient = httpClient;
        this.jsonMapper = jsonMapper;
    }

    /**
     * Search events by classification (Music, Sports, Arts &amp; Theatre).
     * Returns the raw SearchResponse so callers can inspect page info.
     */
    public SearchResponse searchEvents(String classificationName, int size, int page)
            throws IOException, InterruptedException {
        String url = BASE_URL + "/events.json"
                + "?apikey=" + enc(apiKey)
                + "&classificationName=" + enc(classificationName)
                + "&size=" + size
                + "&page=" + page
                + "&sort=relevance,desc";

        String json = fetch(url);
        return jsonMapper.readValue(json, SearchResponse.class);
    }

    /**
     * Search events with a keyword (artist name, team, etc.).
     */
    public SearchResponse searchByKeyword(String keyword, int size)
            throws IOException, InterruptedException {
        String url = BASE_URL + "/events.json"
                + "?apikey=" + enc(apiKey)
                + "&keyword=" + enc(keyword)
                + "&size=" + size
                + "&sort=relevance,desc";

        String json = fetch(url);
        return jsonMapper.readValue(json, SearchResponse.class);
    }

    /**
     * Search events with country code filter (e.g., "US", "CA", "GB").
     */
    public SearchResponse searchByCountry(String classificationName, String countryCode,
                                           int size, int page)
            throws IOException, InterruptedException {
        String url = BASE_URL + "/events.json"
                + "?apikey=" + enc(apiKey)
                + "&classificationName=" + enc(classificationName)
                + "&countryCode=" + enc(countryCode)
                + "&size=" + size
                + "&page=" + page
                + "&sort=relevance,desc";

        String json = fetch(url);
        return jsonMapper.readValue(json, SearchResponse.class);
    }

    /**
     * Search events by keyword with country filter.
     */
    public SearchResponse searchByKeywordAndCountry(String keyword, String countryCode, int size)
            throws IOException, InterruptedException {
        String url = BASE_URL + "/events.json"
                + "?apikey=" + enc(apiKey)
                + "&keyword=" + enc(keyword)
                + "&countryCode=" + enc(countryCode)
                + "&size=" + size
                + "&sort=relevance,desc";

        String json = fetch(url);
        return jsonMapper.readValue(json, SearchResponse.class);
    }

    /**
     * Fetch a single event by its Ticketmaster ID.
     * This returns the full detail including _embedded.attractions with externalLinks.
     */
    public EventResponse getEvent(String eventId) throws IOException, InterruptedException {
        String url = BASE_URL + "/events/" + enc(eventId) + ".json"
                + "?apikey=" + enc(apiKey);

        String json = fetch(url);
        return jsonMapper.readValue(json, EventResponse.class);
    }

    /**
     * Fetch raw JSON for a search — useful for inspecting the actual API shape.
     */
    public String searchEventsRaw(String classificationName, int size)
            throws IOException, InterruptedException {
        String url = BASE_URL + "/events.json"
                + "?apikey=" + enc(apiKey)
                + "&classificationName=" + enc(classificationName)
                + "&size=" + size
                + "&sort=relevance,desc";

        return fetch(url);
    }

    /**
     * Fetch raw JSON for a country-filtered search.
     */
    public String searchEventsRawByCountry(String classificationName, String countryCode, int size)
            throws IOException, InterruptedException {
        String url = BASE_URL + "/events.json"
                + "?apikey=" + enc(apiKey)
                + "&classificationName=" + enc(classificationName)
                + "&countryCode=" + enc(countryCode)
                + "&size=" + size
                + "&sort=relevance,desc";

        return fetch(url);
    }

    /**
     * Fetch raw JSON for a single event — useful for inspecting externalLinks shape.
     */
    public String getEventRaw(String eventId) throws IOException, InterruptedException {
        String url = BASE_URL + "/events/" + enc(eventId) + ".json"
                + "?apikey=" + enc(apiKey);

        return fetch(url);
    }

    /**
     * Pretty-print a JSON string using Jackson 3's JsonMapper.
     */
    public String prettyPrint(String json) {
        Object tree = jsonMapper.readTree(json);
        return jsonMapper.writerWithDefaultPrettyPrinter().writeValueAsString(tree);
    }

    public JsonMapper jsonMapper() {
        return jsonMapper;
    }

    private String fetch(String url) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .timeout(Duration.ofSeconds(30))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 429) {
            throw new IOException("Rate limited by Ticketmaster (429). Retry-After: "
                    + response.headers().firstValue("Retry-After").orElse("unknown"));
        }
        if (response.statusCode() != 200) {
            throw new IOException("Ticketmaster API returned " + response.statusCode()
                    + ": " + response.body().substring(0, Math.min(500, response.body().length())));
        }

        return response.body();
    }

    private static String enc(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
