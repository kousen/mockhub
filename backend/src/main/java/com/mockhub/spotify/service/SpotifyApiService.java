package com.mockhub.spotify.service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.mockhub.spotify.dto.SpotifyArtistDto;

@Service
@Profile("spotify")
@Primary
public class SpotifyApiService implements SpotifyService {

    private static final Logger log = LoggerFactory.getLogger(SpotifyApiService.class);
    private static final long CACHE_TTL_SECONDS = 3600;
    private static final int MAX_RETRIES = 3;
    private static final long BASE_BACKOFF_MS = 1000;
    private static final String SANITIZE_PATTERN = "[\\r\\n]";
    private static final java.util.regex.Pattern SPOTIFY_ID_PATTERN =
            java.util.regex.Pattern.compile("^[a-zA-Z0-9]{22}$");

    private final RestClient authClient;
    private final RestClient apiClient;
    private final String clientId;
    private final String clientSecret;

    private volatile String accessToken;
    private volatile Instant tokenExpiry = Instant.EPOCH;

    private final Map<String, CachedArtist> artistCache = new ConcurrentHashMap<>();

    @Autowired
    public SpotifyApiService(
            @Value("${mockhub.spotify.client-id}") String clientId,
            @Value("${mockhub.spotify.client-secret}") String clientSecret) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.authClient = RestClient.builder()
                .baseUrl("https://accounts.spotify.com")
                .build();
        this.apiClient = RestClient.builder()
                .baseUrl("https://api.spotify.com/v1")
                .build();
    }

    SpotifyApiService(RestClient authClient, RestClient apiClient,
                      String clientId, String clientSecret) {
        this.authClient = authClient;
        this.apiClient = apiClient;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    @Override
    public Optional<SpotifyArtistDto> getArtist(String spotifyArtistId) {
        if (spotifyArtistId == null || !SPOTIFY_ID_PATTERN.matcher(spotifyArtistId).matches()) {
            log.warn("Invalid Spotify artist ID format: {}",
                    spotifyArtistId != null ? spotifyArtistId.replaceAll(SANITIZE_PATTERN, "") : "null");
            return Optional.empty();
        }

        CachedArtist cached = artistCache.get(spotifyArtistId);
        if (cached != null && Instant.now().isBefore(cached.expiry())) {
            log.debug("Spotify artist cache hit for {}", spotifyArtistId);
            return Optional.of(cached.artist());
        }

        String sanitizedId = spotifyArtistId.replaceAll(SANITIZE_PATTERN, "");
        try {
            String token = getAccessToken();
            SpotifyArtistResponse response = fetchWithRetry(spotifyArtistId, token);

            if (response == null) {
                return Optional.empty();
            }

            String imageUrl = (response.images() != null && !response.images().isEmpty())
                    ? response.images().getFirst().url()
                    : null;
            int followerCount = (response.followers() != null)
                    ? response.followers().total()
                    : 0;

            SpotifyArtistDto artist = new SpotifyArtistDto(
                    response.id(),
                    response.name(),
                    response.genres() != null ? response.genres() : List.of(),
                    followerCount,
                    imageUrl
            );

            artistCache.put(spotifyArtistId, new CachedArtist(artist,
                    Instant.now().plusSeconds(CACHE_TTL_SECONDS)));
            log.info("Fetched Spotify artist: {} ({})", artist.name(), sanitizedId);
            return Optional.of(artist);

        } catch (HttpClientErrorException.NotFound _) {
            log.info("Spotify artist not found: {}", sanitizedId);
            return Optional.empty();
        } catch (RestClientException e) {
            throw new RestClientException("Spotify API error for artist " + sanitizedId, e);
        }
    }

    private SpotifyArtistResponse fetchWithRetry(String spotifyArtistId, String token) {
        String sanitizedId = spotifyArtistId.replaceAll(SANITIZE_PATTERN, "");
        for (int attempt = 0; attempt <= MAX_RETRIES; attempt++) {
            try {
                return apiClient.get()
                        .uri("/artists/{id}", spotifyArtistId)
                        .header("Authorization", "Bearer " + token)
                        .retrieve()
                        .body(SpotifyArtistResponse.class);
            } catch (HttpClientErrorException.TooManyRequests e) {
                if (attempt == MAX_RETRIES) {
                    log.error("Spotify rate limit exceeded after {} retries for artist {}",
                            MAX_RETRIES, sanitizedId);
                    throw e;
                }
                long waitMs = parseRetryAfter(e.getResponseHeaders(), attempt);
                log.warn("Spotify rate limited (attempt {}/{}), waiting {}ms",
                        attempt + 1, MAX_RETRIES, waitMs);
                sleep(waitMs);
            }
        }
        throw new RestClientException("Spotify API request failed after retries");
    }

    long parseRetryAfter(HttpHeaders headers, int attempt) {
        if (headers != null) {
            String retryAfter = headers.getFirst("Retry-After");
            if (retryAfter != null) {
                try {
                    return Long.parseLong(retryAfter) * 1000;
                } catch (NumberFormatException _) {
                    // Fall through to exponential backoff
                }
            }
        }
        return BASE_BACKOFF_MS * (1L << attempt);
    }

    void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RestClientException("Interrupted while waiting for Spotify rate limit", e);
        }
    }

    private synchronized String getAccessToken() {
        if (accessToken != null && Instant.now().isBefore(tokenExpiry)) {
            return accessToken;
        }

        String credentials = Base64.getEncoder().encodeToString(
                (clientId + ":" + clientSecret).getBytes(StandardCharsets.UTF_8));

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "client_credentials");

        TokenResponse response = authClient.post()
                .uri("/api/token")
                .header("Authorization", "Basic " + credentials)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(body)
                .retrieve()
                .body(TokenResponse.class);

        if (response == null || response.accessToken() == null) {
            throw new RestClientException("Failed to obtain Spotify access token");
        }

        this.accessToken = response.accessToken();
        this.tokenExpiry = Instant.now().plusSeconds(response.expiresIn() - 60);
        log.info("Obtained Spotify access token (expires in {}s)", response.expiresIn());
        return this.accessToken;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record TokenResponse(
            @JsonProperty("access_token") String accessToken,
            @JsonProperty("token_type") String tokenType,
            @JsonProperty("expires_in") long expiresIn
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record SpotifyArtistResponse(
            String id,
            String name,
            List<String> genres,
            Followers followers,
            List<SpotifyImage> images
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Followers(int total) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record SpotifyImage(String url, int height, int width) {
    }

    private record CachedArtist(SpotifyArtistDto artist, Instant expiry) {
    }
}
