package com.mockhub.spotify.dto;

import java.time.Instant;

public record SpotifyConnectionDto(
        boolean connected,
        boolean scopeUpgradeNeeded,
        String spotifyDisplayName,
        Instant connectedAt
) {

    public static SpotifyConnectionDto notConnected() {
        return new SpotifyConnectionDto(false, false, null, null);
    }
}
