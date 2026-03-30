package com.mockhub.spotify.dto;

import java.util.List;

public record SpotifyListeningDto(
        List<String> topArtistIds,
        List<String> topArtistNames,
        List<String> topGenres,
        List<String> recentlyPlayedArtistIds,
        boolean spotifyConnected,
        boolean scopeUpgradeNeeded
) {

    public static SpotifyListeningDto notConnected() {
        return new SpotifyListeningDto(
                List.of(), List.of(), List.of(), List.of(), false, false);
    }

    public static SpotifyListeningDto needsScopeUpgrade() {
        return new SpotifyListeningDto(
                List.of(), List.of(), List.of(), List.of(), true, true);
    }
}
