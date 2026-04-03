package com.mockhub.tmexplorer.parser;

import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.mockhub.tmexplorer.model.AttractionResponse;
import com.mockhub.tmexplorer.model.AttractionResponse.ExternalLink;
import com.mockhub.tmexplorer.model.EventResponse;

/**
 * Extracts Spotify artist IDs from Ticketmaster attraction data.
 *
 * Ticketmaster's externalLinks.spotify field contains URLs in several formats,
 * not all of which are valid artist URLs. Known shapes from production data:
 *
 * 1. Standard:  https://open.spotify.com/artist/6vWDO969PvNqNYHIOW5v0m  (correct)
 * 2. User URL:  https://open.spotify.com/user/blackjoelewismusic        (not an artist page)
 * 3. Mangled:   https://open.spotify.com/user/spotify:artist:2AACqFGo8... (recoverable!)
 * 4. Playlist:  https://open.spotify.com/playlist/3R4v3NMW8CbErUKNfCQbLm (not an artist)
 * 5. Wrong platform: https://music.apple.com/us/artist/microwave/613522668 (data entry error)
 * 6. Unrelated: https://trapkaraoke.com/ (homepage URL in Spotify field)
 *
 * Spotify artist IDs are 22-character base62 strings.
 */
public class SpotifyIdExtractor {

    private static final Pattern SPOTIFY_ID_PATTERN = Pattern.compile("^[a-zA-Z0-9]{22}$");

    // Standard artist URL: /artist/{22-char-id}
    private static final Pattern ARTIST_URL_PATTERN =
            Pattern.compile("open\\.spotify\\.com/artist/([a-zA-Z0-9]{22})");

    // Mangled URI in user path: /user/spotify:artist:{22-char-id}
    private static final Pattern MANGLED_ARTIST_PATTERN =
            Pattern.compile("open\\.spotify\\.com/user/spotify:artist:([a-zA-Z0-9]{22})");

    /**
     * Extract Spotify artist ID from an event's first attraction.
     */
    public Optional<String> fromEvent(EventResponse event) {
        if (event.embedded() == null || event.embedded().attractions() == null
                || event.embedded().attractions().isEmpty()) {
            return Optional.empty();
        }
        return fromAttraction(event.embedded().attractions().getFirst());
    }

    /**
     * Extract Spotify artist ID from an attraction's externalLinks.
     */
    public Optional<String> fromAttraction(AttractionResponse attraction) {
        if (attraction == null || attraction.externalLinks() == null) {
            return Optional.empty();
        }

        List<ExternalLink> spotifyLinks = attraction.externalLinks().get("spotify");
        if (spotifyLinks == null || spotifyLinks.isEmpty()) {
            return Optional.empty();
        }

        return spotifyLinks.stream()
                .map(ExternalLink::url)
                .filter(url -> url != null)
                .map(this::extractIdFromUrl)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .findFirst();
    }

    /**
     * Extract a Spotify artist ID from a URL.
     *
     * Tries the standard /artist/ path first, then falls back to the mangled
     * /user/spotify:artist: format that appears in some Ticketmaster data.
     *
     * Returns empty for playlists, user profiles, non-Spotify URLs, and other
     * formats that don't contain a recoverable artist ID.
     */
    public Optional<String> extractIdFromUrl(String url) {
        if (url == null || url.isBlank()) {
            return Optional.empty();
        }

        // Try standard /artist/{id} path first
        Matcher artistMatcher = ARTIST_URL_PATTERN.matcher(url);
        if (artistMatcher.find()) {
            return Optional.of(artistMatcher.group(1));
        }

        // Try mangled /user/spotify:artist:{id} format
        Matcher mangledMatcher = MANGLED_ARTIST_PATTERN.matcher(url);
        if (mangledMatcher.find()) {
            return Optional.of(mangledMatcher.group(1));
        }

        return Optional.empty();
    }

    /**
     * Validate that a string looks like a Spotify artist ID.
     */
    public boolean isValidSpotifyId(String id) {
        return id != null && SPOTIFY_ID_PATTERN.matcher(id).matches();
    }
}
