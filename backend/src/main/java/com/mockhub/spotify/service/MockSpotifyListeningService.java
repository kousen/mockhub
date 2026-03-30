package com.mockhub.spotify.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import com.mockhub.spotify.dto.SpotifyListeningDto;

/**
 * Mock implementation that returns hardcoded listening data matching
 * the existing mock artists (Beyonce, Drake, Radiohead) in MockSpotifyService.
 * Used in dev/test without real Spotify credentials.
 */
@Service
@Profile("mock-spotify")
public class MockSpotifyListeningService implements SpotifyListeningService {

    private static final Logger log = LoggerFactory.getLogger(MockSpotifyListeningService.class);

    @Override
    public SpotifyListeningDto getListeningData(Long userId) {
        log.debug("Returning mock Spotify listening data for user {}", userId);
        return new SpotifyListeningDto(
                List.of("06HL4z0CvFAxyc27GXpf02", "3TVXtAsR1Inumwj472S9r4", "4Z8W4fKeB5YxbusRsdQVPb"),
                List.of("Beyonce", "Drake", "Radiohead"),
                List.of("pop", "r&b", "hip hop", "rap", "art rock", "alternative"),
                List.of("06HL4z0CvFAxyc27GXpf02", "3TVXtAsR1Inumwj472S9r4"),
                true,
                false
        );
    }
}
