package com.mockhub.spotify.service;

import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.mockhub.auth.entity.OAuthAccount;
import com.mockhub.auth.entity.User;
import com.mockhub.auth.repository.OAuthAccountRepository;
import com.mockhub.spotify.dto.SpotifyConnectionDto;
import com.mockhub.spotify.repository.SpotifyListeningCacheRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SpotifyConnectionServiceTest {

    @Mock
    private OAuthAccountRepository oAuthAccountRepository;

    @Mock
    private SpotifyListeningCacheRepository cacheRepository;

    @InjectMocks
    private SpotifyConnectionService service;

    @Test
    @DisplayName("getConnectionStatus - no Spotify account - returns not connected")
    void getConnectionStatus_noAccount_returnsNotConnected() {
        when(oAuthAccountRepository.findByUserIdAndProvider(1L, "spotify"))
                .thenReturn(Optional.empty());

        SpotifyConnectionDto result = service.getConnectionStatus(1L);

        assertFalse(result.connected());
        assertFalse(result.scopeUpgradeNeeded());
        assertNull(result.spotifyDisplayName());
    }

    @Test
    @DisplayName("getConnectionStatus - connected with all scopes - returns connected no upgrade")
    void getConnectionStatus_connectedFullScopes_returnsConnected() {
        OAuthAccount account = createAccount("user-read-email,user-top-read,user-read-recently-played");

        when(oAuthAccountRepository.findByUserIdAndProvider(1L, "spotify"))
                .thenReturn(Optional.of(account));

        SpotifyConnectionDto result = service.getConnectionStatus(1L);

        assertTrue(result.connected());
        assertFalse(result.scopeUpgradeNeeded());
        assertEquals("spotify-user-123", result.spotifyDisplayName());
    }

    @Test
    @DisplayName("getConnectionStatus - connected with old scopes - returns scope upgrade needed")
    void getConnectionStatus_connectedOldScopes_returnsScopeUpgradeNeeded() {
        OAuthAccount account = createAccount("user-read-email");

        when(oAuthAccountRepository.findByUserIdAndProvider(1L, "spotify"))
                .thenReturn(Optional.of(account));

        SpotifyConnectionDto result = service.getConnectionStatus(1L);

        assertTrue(result.connected());
        assertTrue(result.scopeUpgradeNeeded());
    }

    @Test
    @DisplayName("getConnectionStatus - connected with null scopes - returns scope upgrade needed")
    void getConnectionStatus_nullScopes_returnsScopeUpgradeNeeded() {
        OAuthAccount account = createAccount(null);

        when(oAuthAccountRepository.findByUserIdAndProvider(1L, "spotify"))
                .thenReturn(Optional.of(account));

        SpotifyConnectionDto result = service.getConnectionStatus(1L);

        assertTrue(result.connected());
        assertTrue(result.scopeUpgradeNeeded());
    }

    @Test
    @DisplayName("disconnect - deletes OAuth account and listening cache")
    void disconnect_deletesAccountAndCache() {
        service.disconnect(1L);

        verify(oAuthAccountRepository).deleteByUserIdAndProvider(1L, "spotify");
        verify(cacheRepository).deleteByUserId(1L);
    }

    private OAuthAccount createAccount(String scopes) {
        User user = new User();
        user.setId(1L);

        OAuthAccount account = new OAuthAccount();
        account.setUser(user);
        account.setProvider("spotify");
        account.setProviderAccountId("spotify-user-123");
        account.setScopesGranted(scopes);
        account.setCreatedAt(Instant.now());
        return account;
    }
}
