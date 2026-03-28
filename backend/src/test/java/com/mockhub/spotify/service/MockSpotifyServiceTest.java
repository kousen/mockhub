package com.mockhub.spotify.service;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.mockhub.spotify.dto.SpotifyArtistDto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MockSpotifyServiceTest {

    private MockSpotifyService service;

    @BeforeEach
    void setUp() {
        service = new MockSpotifyService();
    }

    @Test
    @DisplayName("getArtist - given known artist ID - returns matching artist data")
    void getArtist_givenKnownArtistId_returnsMatchingArtistData() {
        Optional<SpotifyArtistDto> result = service.getArtist("06HL4z0CvFAxyc27GXpf02");

        assertTrue(result.isPresent());
        assertEquals("Beyoncé", result.get().name());
        assertEquals("06HL4z0CvFAxyc27GXpf02", result.get().id());
        assertFalse(result.get().genres().isEmpty());
    }

    @Test
    @DisplayName("getArtist - given unknown artist ID - returns default mock artist")
    void getArtist_givenUnknownArtistId_returnsDefaultMockArtist() {
        Optional<SpotifyArtistDto> result = service.getArtist("unknown-id-123");

        assertTrue(result.isPresent());
        assertEquals("Mock Artist", result.get().name());
        assertEquals("unknown-id-123", result.get().id());
        assertFalse(result.get().genres().isEmpty());
    }

    @Test
    @DisplayName("getArtist - returns artist with genres list")
    void getArtist_returnsArtistWithGenresList() {
        Optional<SpotifyArtistDto> result = service.getArtist("4Z8W4fKeB5YxbusRsdQVPb");

        assertTrue(result.isPresent());
        assertEquals("Radiohead", result.get().name());
        assertTrue(result.get().genres().contains("alternative rock"));
    }
}
