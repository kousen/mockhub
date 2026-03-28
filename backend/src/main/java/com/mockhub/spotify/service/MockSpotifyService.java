package com.mockhub.spotify.service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import com.mockhub.spotify.dto.SpotifyArtistDto;

@Service
@Profile("mock-spotify")
public class MockSpotifyService implements SpotifyService {

    private static final Logger log = LoggerFactory.getLogger(MockSpotifyService.class);

    private static final Map<String, SpotifyArtistDto> MOCK_ARTISTS = Map.of(
            "06HL4z0CvFAxyc27GXpf02", new SpotifyArtistDto(
                    "06HL4z0CvFAxyc27GXpf02", "Beyoncé",
                    List.of("pop", "r&b", "dance pop"), 42_000_000, null),
            "3TVXtAsR1Inumwj472S9r4", new SpotifyArtistDto(
                    "3TVXtAsR1Inumwj472S9r4", "Drake",
                    List.of("canadian hip hop", "hip hop", "rap"), 83_000_000, null),
            "4Z8W4fKeB5YxbusRsdQVPb", new SpotifyArtistDto(
                    "4Z8W4fKeB5YxbusRsdQVPb", "Radiohead",
                    List.of("alternative rock", "art rock", "permanent wave"), 14_000_000, null)
    );

    @Override
    public Optional<SpotifyArtistDto> getArtist(String spotifyArtistId) {
        log.info("[MOCK SPOTIFY] Fetching artist: {}", spotifyArtistId);
        SpotifyArtistDto artist = MOCK_ARTISTS.get(spotifyArtistId);
        if (artist != null) {
            return Optional.of(artist);
        }
        return Optional.of(new SpotifyArtistDto(
                spotifyArtistId, "Mock Artist",
                List.of("rock", "indie"), 1_000_000, null));
    }
}
