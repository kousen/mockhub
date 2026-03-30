package com.mockhub.spotify.service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import com.mockhub.auth.entity.OAuthAccount;
import com.mockhub.auth.repository.OAuthAccountRepository;
import com.mockhub.spotify.dto.SpotifyListeningDto;
import com.mockhub.spotify.entity.SpotifyListeningCache;
import com.mockhub.spotify.repository.SpotifyListeningCacheRepository;

@Service
@Profile("spotify")
public class SpotifyListeningApiService implements SpotifyListeningService {

    private static final Logger log = LoggerFactory.getLogger(SpotifyListeningApiService.class);
    private static final Duration CACHE_TTL = Duration.ofHours(24);
    private static final String REQUIRED_SCOPE = "user-top-read";

    private final OAuthAccountRepository oAuthAccountRepository;
    private final SpotifyListeningCacheRepository cacheRepository;
    private final RestClient restClient;
    private final String clientId;
    private final String clientSecret;

    public SpotifyListeningApiService(
            OAuthAccountRepository oAuthAccountRepository,
            SpotifyListeningCacheRepository cacheRepository,
            @Value("${mockhub.spotify.client-id}") String clientId,
            @Value("${mockhub.spotify.client-secret}") String clientSecret) {
        this.oAuthAccountRepository = oAuthAccountRepository;
        this.cacheRepository = cacheRepository;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.restClient = RestClient.builder()
                .baseUrl("https://api.spotify.com/v1")
                .build();
    }

    @Override
    @Transactional
    public SpotifyListeningDto getListeningData(Long userId) {
        Optional<OAuthAccount> accountOpt = oAuthAccountRepository.findByUserIdAndProvider(userId, "spotify");
        if (accountOpt.isEmpty()) {
            return SpotifyListeningDto.notConnected();
        }

        OAuthAccount account = accountOpt.get();

        if (account.getScopesGranted() == null || !account.getScopesGranted().contains(REQUIRED_SCOPE)) {
            log.info("User {} needs Spotify scope upgrade (current: {})", userId, account.getScopesGranted());
            return SpotifyListeningDto.needsScopeUpgrade();
        }

        // Check cache
        Optional<SpotifyListeningCache> cached = cacheRepository.findByUserId(userId);
        if (cached.isPresent() && isCacheFresh(cached.get())) {
            SpotifyListeningCache cache = cached.get();
            return new SpotifyListeningDto(
                    cache.getTopArtistIds(), cache.getTopArtistNames(),
                    cache.getTopGenres(), cache.getRecentlyPlayedArtistIds(),
                    true, false);
        }

        // Fetch from Spotify
        return fetchAndCache(userId, account, cached.orElse(null));
    }

    private boolean isCacheFresh(SpotifyListeningCache cache) {
        return cache.getFetchedAt() != null
                && Duration.between(cache.getFetchedAt(), Instant.now()).compareTo(CACHE_TTL) < 0;
    }

    private SpotifyListeningDto fetchAndCache(Long userId, OAuthAccount account,
                                               SpotifyListeningCache existingCache) {
        String accessToken = account.getAccessTokenEncrypted();

        try {
            return doFetchAndCache(userId, accessToken, existingCache);
        } catch (HttpClientErrorException.Unauthorized e) {
            log.info("Spotify token expired for user {}, attempting refresh", userId);
            String refreshedToken = refreshAccessToken(account);
            if (refreshedToken == null) {
                log.warn("Spotify token refresh failed for user {}", userId);
                return SpotifyListeningDto.needsScopeUpgrade();
            }
            return doFetchAndCache(userId, refreshedToken, existingCache);
        } catch (Exception e) {
            log.error("Failed to fetch Spotify listening data for user {}: {}", userId, e.getMessage());
            // Return stale cache if available
            if (existingCache != null) {
                return new SpotifyListeningDto(
                        existingCache.getTopArtistIds(), existingCache.getTopArtistNames(),
                        existingCache.getTopGenres(), existingCache.getRecentlyPlayedArtistIds(),
                        true, false);
            }
            return new SpotifyListeningDto(List.of(), List.of(), List.of(), List.of(), true, false);
        }
    }

    @SuppressWarnings("unchecked")
    private SpotifyListeningDto doFetchAndCache(Long userId, String accessToken,
                                                 SpotifyListeningCache existingCache) {
        // Fetch top artists
        Map<String, Object> topArtistsResponse = restClient.get()
                .uri("/me/top/artists?limit=20&time_range=medium_term")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .retrieve()
                .body(Map.class);

        List<String> artistIds = new ArrayList<>();
        List<String> artistNames = new ArrayList<>();
        LinkedHashSet<String> genres = new LinkedHashSet<>();

        if (topArtistsResponse != null && topArtistsResponse.containsKey("items")) {
            List<Map<String, Object>> items = (List<Map<String, Object>>) topArtistsResponse.get("items");
            for (Map<String, Object> artist : items) {
                artistIds.add((String) artist.get("id"));
                artistNames.add((String) artist.get("name"));
                List<String> artistGenres = (List<String>) artist.get("genres");
                if (artistGenres != null) {
                    genres.addAll(artistGenres);
                }
            }
        }

        // Fetch recently played
        Map<String, Object> recentlyPlayedResponse = restClient.get()
                .uri("/me/player/recently-played?limit=50")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .retrieve()
                .body(Map.class);

        LinkedHashSet<String> recentArtistIds = new LinkedHashSet<>();
        if (recentlyPlayedResponse != null && recentlyPlayedResponse.containsKey("items")) {
            List<Map<String, Object>> items = (List<Map<String, Object>>) recentlyPlayedResponse.get("items");
            for (Map<String, Object> item : items) {
                Map<String, Object> track = (Map<String, Object>) item.get("track");
                if (track != null) {
                    List<Map<String, Object>> artists = (List<Map<String, Object>>) track.get("artists");
                    if (artists != null) {
                        for (Map<String, Object> artist : artists) {
                            recentArtistIds.add((String) artist.get("id"));
                        }
                    }
                }
            }
        }

        // Upsert cache
        SpotifyListeningCache cache = existingCache != null ? existingCache : new SpotifyListeningCache();
        cache.setUserId(userId);
        cache.setTopArtistIds(artistIds);
        cache.setTopArtistNames(artistNames);
        cache.setTopGenres(new ArrayList<>(genres));
        cache.setRecentlyPlayedArtistIds(new ArrayList<>(recentArtistIds));
        cache.setFetchedAt(Instant.now());
        cacheRepository.save(cache);

        log.info("Cached Spotify listening data for user {}: {} top artists, {} genres, {} recent artists",
                userId, artistIds.size(), genres.size(), recentArtistIds.size());

        return new SpotifyListeningDto(
                artistIds, artistNames, new ArrayList<>(genres),
                new ArrayList<>(recentArtistIds), true, false);
    }

    @SuppressWarnings("unchecked")
    private String refreshAccessToken(OAuthAccount account) {
        String refreshToken = account.getRefreshTokenEncrypted();
        if (refreshToken == null || refreshToken.isBlank()) {
            return null;
        }

        try {
            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("grant_type", "refresh_token");
            body.add("refresh_token", refreshToken);
            body.add("client_id", clientId);
            body.add("client_secret", clientSecret);

            Map<String, Object> tokenResponse = RestClient.create()
                    .post()
                    .uri("https://accounts.spotify.com/api/token")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(body)
                    .retrieve()
                    .body(Map.class);

            if (tokenResponse == null || !tokenResponse.containsKey("access_token")) {
                return null;
            }

            String newAccessToken = (String) tokenResponse.get("access_token");
            account.setAccessTokenEncrypted(newAccessToken);

            Object expiresIn = tokenResponse.get("expires_in");
            if (expiresIn instanceof Number expiresInNum) {
                account.setTokenExpiresAt(Instant.now().plusSeconds(expiresInNum.longValue()));
            }

            // Spotify may return a new refresh token
            String newRefreshToken = (String) tokenResponse.get("refresh_token");
            if (newRefreshToken != null) {
                account.setRefreshTokenEncrypted(newRefreshToken);
            }

            oAuthAccountRepository.save(account);
            log.info("Refreshed Spotify access token for user {}", account.getUser().getEmail());
            return newAccessToken;
        } catch (Exception e) {
            log.error("Failed to refresh Spotify token: {}", e.getMessage());
            return null;
        }
    }
}
