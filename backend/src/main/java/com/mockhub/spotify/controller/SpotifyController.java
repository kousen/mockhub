package com.mockhub.spotify.controller;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClientException;

import com.mockhub.auth.security.SecurityUser;
import com.mockhub.spotify.dto.SpotifyArtistDto;
import com.mockhub.spotify.dto.SpotifyConnectionDto;
import com.mockhub.spotify.service.SpotifyConnectionService;
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
    private final SpotifyConnectionService connectionService;

    public SpotifyController(Optional<SpotifyService> spotifyService,
                             SpotifyConnectionService connectionService) {
        this.spotifyService = spotifyService;
        this.connectionService = connectionService;
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
                    .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
        } catch (RestClientException e) {
            log.error("Spotify upstream error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).build();
        }
    }

    @GetMapping("/connection")
    @Operation(summary = "Get Spotify connection status",
            description = "Returns whether the user has connected their Spotify account and if scope upgrade is needed")
    @ApiResponse(responseCode = "200", description = "Connection status returned")
    public ResponseEntity<SpotifyConnectionDto> getConnectionStatus(
            @AuthenticationPrincipal SecurityUser securityUser) {
        return ResponseEntity.ok(connectionService.getConnectionStatus(securityUser.getId()));
    }

    @DeleteMapping("/connection")
    @Operation(summary = "Disconnect Spotify",
            description = "Removes Spotify OAuth tokens and listening cache for the user")
    @ApiResponse(responseCode = "204", description = "Spotify disconnected")
    public ResponseEntity<Void> disconnect(@AuthenticationPrincipal SecurityUser securityUser) {
        connectionService.disconnect(securityUser.getId());
        return ResponseEntity.noContent().build();
    }
}
