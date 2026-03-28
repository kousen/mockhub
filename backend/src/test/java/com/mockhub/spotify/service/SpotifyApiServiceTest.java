package com.mockhub.spotify.service;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.mockhub.spotify.dto.SpotifyArtistDto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withResourceNotFound;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class SpotifyApiServiceTest {

    private MockRestServiceServer authServer;
    private MockRestServiceServer apiServer;
    private SpotifyApiService service;

    @BeforeEach
    void setUp() {
        RestClient.Builder authBuilder = RestClient.builder().baseUrl("https://accounts.spotify.com");
        RestClient.Builder apiBuilder = RestClient.builder().baseUrl("https://api.spotify.com/v1");

        authServer = MockRestServiceServer.bindTo(authBuilder).build();
        apiServer = MockRestServiceServer.bindTo(apiBuilder).build();

        service = new SpotifyApiService(
                authBuilder.build(), apiBuilder.build(),
                "fake-id", "fake-secret");
    }

    @Test
    @DisplayName("getArtist - given valid artist ID - returns artist metadata")
    void getArtist_givenValidArtistId_returnsArtistMetadata() {
        stubTokenResponse();
        apiServer.expect(requestTo("https://api.spotify.com/v1/artists/abc123"))
                .andExpect(header("Authorization", "Bearer mock-access-token"))
                .andRespond(withSuccess("""
                        {
                            "id": "abc123",
                            "name": "Test Artist",
                            "genres": ["rock", "indie"],
                            "followers": { "total": 500000 },
                            "images": [
                                { "url": "https://img.spotify.com/large.jpg", "height": 640, "width": 640 },
                                { "url": "https://img.spotify.com/small.jpg", "height": 64, "width": 64 }
                            ]
                        }
                        """, MediaType.APPLICATION_JSON));

        Optional<SpotifyArtistDto> result = service.getArtist("abc123");

        assertTrue(result.isPresent());
        SpotifyArtistDto artist = result.get();
        assertEquals("abc123", artist.id());
        assertEquals("Test Artist", artist.name());
        assertEquals(List.of("rock", "indie"), artist.genres());
        assertEquals(500000, artist.followers());
        assertEquals("https://img.spotify.com/large.jpg", artist.imageUrl());
    }

    @Test
    @DisplayName("getArtist - given 404 from Spotify - returns empty")
    void getArtist_given404FromSpotify_returnsEmpty() {
        stubTokenResponse();
        apiServer.expect(requestTo("https://api.spotify.com/v1/artists/bad-id"))
                .andRespond(withResourceNotFound());

        Optional<SpotifyArtistDto> result = service.getArtist("bad-id");

        assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("getArtist - given 500 from Spotify - throws RestClientException")
    void getArtist_given500FromSpotify_throwsRestClientException() {
        stubTokenResponse();
        apiServer.expect(requestTo("https://api.spotify.com/v1/artists/error-id"))
                .andRespond(withServerError());

        assertThrows(RestClientException.class, () -> service.getArtist("error-id"));
    }

    @Test
    @DisplayName("getArtist - given cached artist - does not call API again")
    void getArtist_givenCachedArtist_doesNotCallApiAgain() {
        stubTokenResponse();
        apiServer.expect(requestTo("https://api.spotify.com/v1/artists/cached123"))
                .andRespond(withSuccess("""
                        {
                            "id": "cached123",
                            "name": "Cached Artist",
                            "genres": ["pop"],
                            "followers": { "total": 100 },
                            "images": []
                        }
                        """, MediaType.APPLICATION_JSON));

        service.getArtist("cached123");
        // Second call should use cache, not hit the server
        Optional<SpotifyArtistDto> result = service.getArtist("cached123");

        assertTrue(result.isPresent());
        assertEquals("Cached Artist", result.get().name());
        authServer.verify();
        apiServer.verify();
    }

    @Test
    @DisplayName("getArtist - given artist with null genres and followers - handles gracefully")
    void getArtist_givenArtistWithNullFields_handlesGracefully() {
        stubTokenResponse();
        apiServer.expect(requestTo("https://api.spotify.com/v1/artists/minimal"))
                .andRespond(withSuccess("""
                        {
                            "id": "minimal",
                            "name": "Minimal Artist",
                            "genres": null,
                            "followers": null,
                            "images": null
                        }
                        """, MediaType.APPLICATION_JSON));

        Optional<SpotifyArtistDto> result = service.getArtist("minimal");

        assertTrue(result.isPresent());
        SpotifyArtistDto artist = result.get();
        assertEquals("Minimal Artist", artist.name());
        assertTrue(artist.genres().isEmpty());
        assertEquals(0, artist.followers());
        assertNull(artist.imageUrl());
    }

    @Test
    @DisplayName("getArtist - reuses token for second call")
    void getArtist_reusesTokenForSecondCall() {
        stubTokenResponse();
        apiServer.expect(requestTo("https://api.spotify.com/v1/artists/first"))
                .andRespond(withSuccess("""
                        { "id": "first", "name": "First", "genres": [], "followers": { "total": 1 }, "images": [] }
                        """, MediaType.APPLICATION_JSON));
        apiServer.expect(requestTo("https://api.spotify.com/v1/artists/second"))
                .andRespond(withSuccess("""
                        { "id": "second", "name": "Second", "genres": [], "followers": { "total": 2 }, "images": [] }
                        """, MediaType.APPLICATION_JSON));

        service.getArtist("first");
        Optional<SpotifyArtistDto> result = service.getArtist("second");

        assertTrue(result.isPresent());
        assertEquals("Second", result.get().name());
        // Only one token request should have been made
        authServer.verify();
    }

    private void stubTokenResponse() {
        authServer.expect(requestTo("https://accounts.spotify.com/api/token"))
                .andExpect(header("Authorization", "Basic " +
                        Base64.getEncoder().encodeToString("fake-id:fake-secret".getBytes(StandardCharsets.UTF_8))))
                .andRespond(withSuccess("""
                        {
                            "access_token": "mock-access-token",
                            "token_type": "bearer",
                            "expires_in": 3600
                        }
                        """, MediaType.APPLICATION_JSON));
    }
}
