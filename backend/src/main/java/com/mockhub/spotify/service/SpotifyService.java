package com.mockhub.spotify.service;

import java.util.Optional;

import com.mockhub.spotify.dto.SpotifyArtistDto;

public interface SpotifyService {

    Optional<SpotifyArtistDto> getArtist(String spotifyArtistId);
}
