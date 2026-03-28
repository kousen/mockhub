package com.mockhub.spotify.controller;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClientException;

import com.mockhub.spotify.dto.SpotifyArtistDto;
import com.mockhub.spotify.service.SpotifyService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/spotify")
@Tag(name = "Spotify", description = "Spotify artist metadata")
public class SpotifyController {

    private static final Logger log = LoggerFactory.getLogger(SpotifyController.class);

    private final Optional<SpotifyService> spotifyService;

    public SpotifyController(Optional<SpotifyService> spotifyService) {
        this.spotifyService = spotifyService;
    }

    @GetMapping("/artists/{spotifyArtistId}")
    @Operation(summary = "Get Spotify artist metadata",
            description = "Returns artist name, genres, follower count, and image URL from Spotify")
    @ApiResponse(responseCode = "200", description = "Artist metadata returned")
    @ApiResponse(responseCode = "404", description = "Artist not found on Spotify")
    @ApiResponse(responseCode = "502", description = "Spotify API error")
    @ApiResponse(responseCode = "503", description = "Spotify integration not enabled")
    public ResponseEntity<SpotifyArtistDto> getArtist(@PathVariable String spotifyArtistId) {
        if (spotifyService.isEmpty()) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }
        try {
            return spotifyService.get().getArtist(spotifyArtistId)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (RestClientException e) {
            log.error("Spotify upstream error for artist {}: {}",
                    spotifyArtistId.replaceAll("[\\r\\n]", ""), e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).build();
        }
    }
}
