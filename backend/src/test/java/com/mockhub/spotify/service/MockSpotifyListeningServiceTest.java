package com.mockhub.spotify.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.mockhub.spotify.dto.SpotifyListeningDto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MockSpotifyListeningServiceTest {

    private final MockSpotifyListeningService service = new MockSpotifyListeningService();

    @Test
    @DisplayName("getListeningData - returns mock artists matching MockSpotifyService")
    void getListeningData_returnsMockArtists() {
        SpotifyListeningDto result = service.getListeningData(1L);

        assertEquals(3, result.topArtistIds().size());
        assertTrue(result.topArtistNames().contains("Beyonce"));
        assertTrue(result.topArtistNames().contains("Drake"));
        assertTrue(result.topArtistNames().contains("Radiohead"));
    }

    @Test
    @DisplayName("getListeningData - returns genres from mock artists")
    void getListeningData_returnsGenres() {
        SpotifyListeningDto result = service.getListeningData(1L);

        assertFalse(result.topGenres().isEmpty());
        assertTrue(result.topGenres().contains("pop"));
    }

    @Test
    @DisplayName("getListeningData - shows spotify connected without scope upgrade needed")
    void getListeningData_spotifyConnectedNoScopeUpgrade() {
        SpotifyListeningDto result = service.getListeningData(1L);

        assertTrue(result.spotifyConnected());
        assertFalse(result.scopeUpgradeNeeded());
    }

    @Test
    @DisplayName("getListeningData - returns recently played artist IDs")
    void getListeningData_returnsRecentlyPlayed() {
        SpotifyListeningDto result = service.getListeningData(1L);

        assertFalse(result.recentlyPlayedArtistIds().isEmpty());
    }
}
