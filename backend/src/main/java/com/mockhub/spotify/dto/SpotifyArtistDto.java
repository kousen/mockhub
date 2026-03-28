package com.mockhub.spotify.dto;

import java.util.List;

public record SpotifyArtistDto(
        String id,
        String name,
        List<String> genres,
        int followers,
        String imageUrl
) {
}
