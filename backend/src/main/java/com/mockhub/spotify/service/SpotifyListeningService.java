package com.mockhub.spotify.service;

import com.mockhub.spotify.dto.SpotifyListeningDto;

/**
 * Retrieves a user's Spotify listening data (top artists, genres, recently played).
 * Profile-based implementations: SpotifyListeningApiService (real) and
 * MockSpotifyListeningService (dev/test).
 */
public interface SpotifyListeningService {

    SpotifyListeningDto getListeningData(Long userId);
}
