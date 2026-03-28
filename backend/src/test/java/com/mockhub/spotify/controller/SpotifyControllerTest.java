package com.mockhub.spotify.controller;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.mockhub.auth.repository.UserRepository;
import com.mockhub.auth.security.JwtAuthenticationFilter;
import com.mockhub.auth.security.JwtTokenProvider;
import com.mockhub.auth.security.UserDetailsServiceImpl;
import com.mockhub.config.SecurityConfig;
import com.mockhub.spotify.dto.SpotifyArtistDto;
import com.mockhub.spotify.service.SpotifyService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;
import org.springframework.web.client.RestClientException;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SpotifyController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class SpotifyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SpotifyService spotifyService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private UserDetailsServiceImpl userDetailsService;

    @Test
    @DisplayName("GET /api/v1/spotify/artists/{id} - given valid artist - returns 200 with metadata")
    void getArtist_givenValidArtist_returns200WithMetadata() throws Exception {
        SpotifyArtistDto artist = new SpotifyArtistDto(
                "abc123", "Test Artist", List.of("rock", "indie"),
                500_000, "https://img.spotify.com/artist.jpg");
        when(spotifyService.getArtist("abc123")).thenReturn(Optional.of(artist));

        mockMvc.perform(get("/api/v1/spotify/artists/abc123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("abc123"))
                .andExpect(jsonPath("$.name").value("Test Artist"))
                .andExpect(jsonPath("$.genres[0]").value("rock"))
                .andExpect(jsonPath("$.followers").value(500000))
                .andExpect(jsonPath("$.imageUrl").value("https://img.spotify.com/artist.jpg"));
    }

    @Test
    @DisplayName("GET /api/v1/spotify/artists/{id} - given unknown artist - returns 404")
    void getArtist_givenUnknownArtist_returns404() throws Exception {
        when(spotifyService.getArtist("unknown")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/spotify/artists/unknown"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/v1/spotify/artists/{id} - given Spotify upstream error - returns 502")
    void getArtist_givenUpstreamError_returns502() throws Exception {
        when(spotifyService.getArtist("error-id"))
                .thenThrow(new RestClientException("Spotify is down"));

        mockMvc.perform(get("/api/v1/spotify/artists/error-id"))
                .andExpect(status().isBadGateway());
    }

    @Test
    @DisplayName("GET /api/v1/spotify/artists/{id} - is publicly accessible without auth")
    void getArtist_isPubliclyAccessible() throws Exception {
        SpotifyArtistDto artist = new SpotifyArtistDto(
                "abc123", "Test Artist", List.of("rock"), 100, null);
        when(spotifyService.getArtist("abc123")).thenReturn(Optional.of(artist));

        mockMvc.perform(get("/api/v1/spotify/artists/abc123"))
                .andExpect(status().isOk());
    }

    @Nested
    @DisplayName("Unit tests for SpotifyController without Spring context")
    class UnitTests {

        @Test
        @DisplayName("getArtist - given no SpotifyService bean - returns 503")
        void getArtist_givenNoSpotifyService_returns503() {
            SpotifyController controller = new SpotifyController(Optional.empty());

            ResponseEntity<SpotifyArtistDto> response = controller.getArtist("abc123");

            assertEquals(503, response.getStatusCode().value());
        }
    }
}
