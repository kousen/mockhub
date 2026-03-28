package com.mockhub.spotify.service;

import java.time.Instant;
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
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
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
        CachedArtist cached = artistCache.get(spotifyArtistId);
        if (cached != null && Instant.now().isBefore(cached.expiry())) {
            log.debug("Spotify artist cache hit for {}", spotifyArtistId);
            return Optional.of(cached.artist());
        }

        try {
            String token = getAccessToken();
            SpotifyArtistResponse response = apiClient.get()
                    .uri("/artists/{id}", spotifyArtistId)
                    .header("Authorization", "Bearer " + token)
                    .retrieve()
                    .body(SpotifyArtistResponse.class);

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
            log.info("Fetched Spotify artist: {} ({})", artist.name(), spotifyArtistId);
            return Optional.of(artist);

        } catch (RestClientException e) {
            log.error("Failed to fetch Spotify artist {}: {}", spotifyArtistId, e.getMessage());
            return Optional.empty();
        }
    }

    private synchronized String getAccessToken() {
        if (accessToken != null && Instant.now().isBefore(tokenExpiry)) {
            return accessToken;
        }

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "client_credentials");
        body.add("client_id", clientId);
        body.add("client_secret", clientSecret);

        TokenResponse response = authClient.post()
                .uri("/api/token")
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
