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
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.mockhub.auth.entity.OAuthAccount;
import com.mockhub.auth.entity.User;
import com.mockhub.auth.repository.OAuthAccountRepository;
import com.mockhub.spotify.dto.SpotifyListeningDto;
import com.mockhub.spotify.entity.SpotifyListeningCache;
import com.mockhub.spotify.repository.SpotifyListeningCacheRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withUnauthorizedRequest;

@ExtendWith(MockitoExtension.class)
class SpotifyListeningApiServiceTest {

    @Mock
    private OAuthAccountRepository oAuthAccountRepository;

    @Mock
    private SpotifyListeningCacheRepository cacheRepository;

    private SpotifyListeningApiService service;
    private MockRestServiceServer mockApiServer;
    private MockRestServiceServer mockTokenServer;

    private static final String ALL_SCOPES = "user-read-email,user-top-read,user-read-recently-played";

    private static final String TOP_ARTISTS_JSON = """
            {
              "items": [
                {"id": "artist-1", "name": "Artist One", "genres": ["rock", "indie"]},
                {"id": "artist-2", "name": "Artist Two", "genres": ["pop"]}
              ]
            }
            """;

    private static final String RECENTLY_PLAYED_JSON = """
            {
              "items": [
                {"track": {"artists": [{"id": "artist-1"}]}},
                {"track": {"artists": [{"id": "artist-3"}]}}
              ]
            }
            """;

    private static final String TOKEN_RESPONSE_JSON = """
            {"access_token": "new-access-token", "expires_in": 3600, "refresh_token": "new-refresh-token"}
            """;

    @BeforeEach
    void setUp() {
        RestClient.Builder apiBuilder = RestClient.builder().baseUrl("https://api.spotify.com/v1");
        mockApiServer = MockRestServiceServer.bindTo(apiBuilder).build();
        RestClient apiClient = apiBuilder.build();

        RestClient.Builder tokenBuilder = RestClient.builder().baseUrl("https://accounts.spotify.com");
        mockTokenServer = MockRestServiceServer.bindTo(tokenBuilder).build();
        RestClient tokenClient = tokenBuilder.build();

        service = new SpotifyListeningApiService(
                oAuthAccountRepository, cacheRepository,
                "test-client-id", "test-client-secret", apiClient, tokenClient);
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
        OAuthAccount account = createSpotifyAccount(ALL_SCOPES);

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
    @DisplayName("getListeningData - stale cache - fetches from Spotify and updates cache")
    void getListeningData_staleCache_fetchesFromSpotify() {
        OAuthAccount account = createSpotifyAccount(ALL_SCOPES);
        account.setAccessTokenEncrypted("valid-token");

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
        when(cacheRepository.save(any(SpotifyListeningCache.class))).thenAnswer(i -> i.getArgument(0));

        mockApiServer.expect(requestTo("https://api.spotify.com/v1/me/top/artists?limit=20&time_range=medium_term"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(TOP_ARTISTS_JSON, MediaType.APPLICATION_JSON));

        mockApiServer.expect(requestTo("https://api.spotify.com/v1/me/player/recently-played?limit=50"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(RECENTLY_PLAYED_JSON, MediaType.APPLICATION_JSON));

        SpotifyListeningDto result = service.getListeningData(1L);

        assertTrue(result.spotifyConnected());
        assertFalse(result.scopeUpgradeNeeded());
        assertEquals(List.of("artist-1", "artist-2"), result.topArtistIds());
        assertEquals(List.of("Artist One", "Artist Two"), result.topArtistNames());
        assertTrue(result.topGenres().contains("rock"));
        assertTrue(result.recentlyPlayedArtistIds().contains("artist-3"));

        verify(cacheRepository).save(any(SpotifyListeningCache.class));
        mockApiServer.verify();
    }

    @Test
    @DisplayName("getListeningData - no cache, fresh token - fetches and caches")
    void getListeningData_noCache_fetchesAndCaches() {
        OAuthAccount account = createSpotifyAccount(ALL_SCOPES);
        account.setAccessTokenEncrypted("valid-token");

        when(oAuthAccountRepository.findByUserIdAndProvider(1L, "spotify"))
                .thenReturn(Optional.of(account));
        when(cacheRepository.findByUserId(1L)).thenReturn(Optional.empty());
        when(cacheRepository.save(any(SpotifyListeningCache.class))).thenAnswer(i -> i.getArgument(0));

        mockApiServer.expect(requestTo("https://api.spotify.com/v1/me/top/artists?limit=20&time_range=medium_term"))
                .andRespond(withSuccess(TOP_ARTISTS_JSON, MediaType.APPLICATION_JSON));

        mockApiServer.expect(requestTo("https://api.spotify.com/v1/me/player/recently-played?limit=50"))
                .andRespond(withSuccess(RECENTLY_PLAYED_JSON, MediaType.APPLICATION_JSON));

        SpotifyListeningDto result = service.getListeningData(1L);

        assertEquals(2, result.topArtistIds().size());
        verify(cacheRepository).save(any(SpotifyListeningCache.class));
        mockApiServer.verify();
    }

    @Test
    @DisplayName("getListeningData - 401 and refresh fails - returns scope upgrade needed")
    void getListeningData_unauthorizedRefreshFails_returnsScopeUpgrade() {
        OAuthAccount account = createSpotifyAccount(ALL_SCOPES);
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

        mockApiServer.expect(requestTo("https://api.spotify.com/v1/me/top/artists?limit=20&time_range=medium_term"))
                .andRespond(withUnauthorizedRequest());

        // Refresh fails (no refresh token, no token endpoint mocked)
        // Returns scopeUpgradeNeeded so user re-authorizes
        SpotifyListeningDto result = service.getListeningData(1L);

        assertTrue(result.scopeUpgradeNeeded());
    }

    @Test
    @DisplayName("getListeningData - 401 then refresh succeeds - retries and returns data")
    void getListeningData_unauthorizedThenRefresh_retriesSuccessfully() {
        OAuthAccount account = createSpotifyAccount(ALL_SCOPES);
        account.setAccessTokenEncrypted("expired-token");
        account.setRefreshTokenEncrypted("valid-refresh-token");

        when(oAuthAccountRepository.findByUserIdAndProvider(1L, "spotify"))
                .thenReturn(Optional.of(account));
        when(cacheRepository.findByUserId(1L)).thenReturn(Optional.empty());
        when(oAuthAccountRepository.save(any(OAuthAccount.class))).thenReturn(account);
        when(cacheRepository.save(any(SpotifyListeningCache.class))).thenAnswer(i -> i.getArgument(0));

        // First call returns 401
        mockApiServer.expect(requestTo("https://api.spotify.com/v1/me/top/artists?limit=20&time_range=medium_term"))
                .andRespond(withUnauthorizedRequest());

        // Token refresh succeeds
        mockTokenServer.expect(requestTo("https://accounts.spotify.com/api/token"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(TOKEN_RESPONSE_JSON, MediaType.APPLICATION_JSON));

        // Retry with new token succeeds
        mockApiServer.expect(requestTo("https://api.spotify.com/v1/me/top/artists?limit=20&time_range=medium_term"))
                .andRespond(withSuccess(TOP_ARTISTS_JSON, MediaType.APPLICATION_JSON));
        mockApiServer.expect(requestTo("https://api.spotify.com/v1/me/player/recently-played?limit=50"))
                .andRespond(withSuccess(RECENTLY_PLAYED_JSON, MediaType.APPLICATION_JSON));

        SpotifyListeningDto result = service.getListeningData(1L);

        assertFalse(result.scopeUpgradeNeeded());
        assertEquals(2, result.topArtistIds().size());
        assertEquals("new-access-token", account.getAccessTokenEncrypted());

        mockApiServer.verify();
        mockTokenServer.verify();
    }

    @Test
    @DisplayName("getListeningData - API error, no cache - returns empty connected data")
    void getListeningData_apiError_noCache_returnsEmptyConnected() {
        OAuthAccount account = createSpotifyAccount(ALL_SCOPES);
        account.setAccessTokenEncrypted("bad-token");

        when(oAuthAccountRepository.findByUserIdAndProvider(1L, "spotify"))
                .thenReturn(Optional.of(account));
        when(cacheRepository.findByUserId(1L)).thenReturn(Optional.empty());

        mockApiServer.expect(requestTo("https://api.spotify.com/v1/me/top/artists?limit=20&time_range=medium_term"))
                .andRespond(withUnauthorizedRequest());

        SpotifyListeningDto result = service.getListeningData(1L);

        assertTrue(result.spotifyConnected());
        assertTrue(result.topArtistIds().isEmpty());
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
