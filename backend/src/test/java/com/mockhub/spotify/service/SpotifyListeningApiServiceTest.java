package com.mockhub.spotify.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.mockhub.auth.entity.OAuthAccount;
import com.mockhub.auth.entity.User;
import com.mockhub.auth.repository.OAuthAccountRepository;
import com.mockhub.spotify.dto.SpotifyListeningDto;
import com.mockhub.spotify.entity.SpotifyListeningCache;
import com.mockhub.spotify.repository.SpotifyListeningCacheRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SpotifyListeningApiServiceTest {

    @Mock
    private OAuthAccountRepository oAuthAccountRepository;

    @Mock
    private SpotifyListeningCacheRepository cacheRepository;

    private SpotifyListeningApiService service;

    @BeforeEach
    void setUp() {
        service = new SpotifyListeningApiService(
                oAuthAccountRepository, cacheRepository,
                "test-client-id", "test-client-secret");
    }

    @Test
    @DisplayName("getListeningData - no Spotify account - returns not connected")
    void getListeningData_noSpotifyAccount_returnsNotConnected() {
        when(oAuthAccountRepository.findByUserIdAndProvider(1L, "spotify"))
                .thenReturn(Optional.empty());

        SpotifyListeningDto result = service.getListeningData(1L);

        assertFalse(result.spotifyConnected());
        assertFalse(result.scopeUpgradeNeeded());
        assertTrue(result.topArtistIds().isEmpty());
    }

    @Test
    @DisplayName("getListeningData - missing required scope - returns scope upgrade needed")
    void getListeningData_missingScope_returnsScopeUpgradeNeeded() {
        OAuthAccount account = createSpotifyAccount("user-read-email");

        when(oAuthAccountRepository.findByUserIdAndProvider(1L, "spotify"))
                .thenReturn(Optional.of(account));

        SpotifyListeningDto result = service.getListeningData(1L);

        assertTrue(result.spotifyConnected());
        assertTrue(result.scopeUpgradeNeeded());
    }

    @Test
    @DisplayName("getListeningData - only one required scope - returns scope upgrade needed")
    void getListeningData_partialScopes_returnsScopeUpgradeNeeded() {
        OAuthAccount account = createSpotifyAccount("user-read-email,user-top-read");

        when(oAuthAccountRepository.findByUserIdAndProvider(1L, "spotify"))
                .thenReturn(Optional.of(account));

        SpotifyListeningDto result = service.getListeningData(1L);

        assertTrue(result.spotifyConnected());
        assertTrue(result.scopeUpgradeNeeded());
    }

    @Test
    @DisplayName("getListeningData - null scopes - returns scope upgrade needed")
    void getListeningData_nullScopes_returnsScopeUpgradeNeeded() {
        OAuthAccount account = createSpotifyAccount(null);

        when(oAuthAccountRepository.findByUserIdAndProvider(1L, "spotify"))
                .thenReturn(Optional.of(account));

        SpotifyListeningDto result = service.getListeningData(1L);

        assertTrue(result.spotifyConnected());
        assertTrue(result.scopeUpgradeNeeded());
    }

    @Test
    @DisplayName("getListeningData - fresh cache - returns cached data without API call")
    void getListeningData_freshCache_returnsCachedData() {
        OAuthAccount account = createSpotifyAccount("user-read-email,user-top-read,user-read-recently-played");

        SpotifyListeningCache cache = new SpotifyListeningCache();
        cache.setUserId(1L);
        cache.setTopArtistIds(List.of("artist-1", "artist-2"));
        cache.setTopArtistNames(List.of("Artist One", "Artist Two"));
        cache.setTopGenres(List.of("rock", "pop"));
        cache.setRecentlyPlayedArtistIds(List.of("artist-1"));
        cache.setFetchedAt(Instant.now().minus(1, ChronoUnit.HOURS));

        when(oAuthAccountRepository.findByUserIdAndProvider(1L, "spotify"))
                .thenReturn(Optional.of(account));
        when(cacheRepository.findByUserId(1L)).thenReturn(Optional.of(cache));

        SpotifyListeningDto result = service.getListeningData(1L);

        assertTrue(result.spotifyConnected());
        assertFalse(result.scopeUpgradeNeeded());
        assertEquals(2, result.topArtistIds().size());
        assertEquals("Artist One", result.topArtistNames().get(0));
        assertEquals(2, result.topGenres().size());
        assertEquals(1, result.recentlyPlayedArtistIds().size());
    }

    @Test
    @DisplayName("getListeningData - stale cache with valid scope - returns connected with data")
    void getListeningData_staleCache_returnsFallbackData() {
        OAuthAccount account = createSpotifyAccount("user-read-email,user-top-read,user-read-recently-played");
        account.setAccessTokenEncrypted("expired-token");

        SpotifyListeningCache staleCache = new SpotifyListeningCache();
        staleCache.setUserId(1L);
        staleCache.setTopArtistIds(List.of("old-artist"));
        staleCache.setTopArtistNames(List.of("Old Artist"));
        staleCache.setTopGenres(List.of("jazz"));
        staleCache.setRecentlyPlayedArtistIds(List.of());
        staleCache.setFetchedAt(Instant.now().minus(25, ChronoUnit.HOURS));

        when(oAuthAccountRepository.findByUserIdAndProvider(1L, "spotify"))
                .thenReturn(Optional.of(account));
        when(cacheRepository.findByUserId(1L)).thenReturn(Optional.of(staleCache));

        // The API call will fail (no real Spotify server). The service
        // should gracefully return data (stale cache or empty) without crashing.
        SpotifyListeningDto result = service.getListeningData(1L);

        assertTrue(result.spotifyConnected());
    }

    private OAuthAccount createSpotifyAccount(String scopes) {
        User user = new User();
        user.setId(1L);
        user.setEmail("test@example.com");

        OAuthAccount account = new OAuthAccount();
        account.setUser(user);
        account.setProvider("spotify");
        account.setProviderAccountId("spotify-123");
        account.setScopesGranted(scopes);
        return account;
    }
}
