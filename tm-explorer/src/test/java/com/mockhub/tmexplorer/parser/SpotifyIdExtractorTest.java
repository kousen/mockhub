package com.mockhub.tmexplorer.parser;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.mockhub.tmexplorer.model.AttractionResponse;
import com.mockhub.tmexplorer.model.AttractionResponse.ExternalLink;
import com.mockhub.tmexplorer.model.SearchResponse;

import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

class SpotifyIdExtractorTest {

    private SpotifyIdExtractor extractor;
    private JsonMapper jsonMapper;

    @BeforeEach
    void setUp() {
        extractor = new SpotifyIdExtractor();
        jsonMapper = JsonMapper.builder()
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .build();
    }

    private String loadFixture(String filename) throws Exception {
        try (InputStream is = getClass().getResourceAsStream("/" + filename)) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @Nested
    @DisplayName("URL parsing")
    class UrlParsing {

        @Test
        @DisplayName("should extract ID from standard Spotify artist URL")
        void standardUrl() {
            Optional<String> id = extractor.extractIdFromUrl(
                    "https://open.spotify.com/artist/6vWDO969PvNqNYHIOW5v0m");
            assertThat(id).contains("6vWDO969PvNqNYHIOW5v0m");
        }

        @Test
        @DisplayName("should extract ID from URL with query params")
        void urlWithQueryParams() {
            Optional<String> id = extractor.extractIdFromUrl(
                    "https://open.spotify.com/artist/06HL4z0CvFAxyc27GXpf02?si=abc123");
            assertThat(id).contains("06HL4z0CvFAxyc27GXpf02");
        }

        @Test
        @DisplayName("should extract ID from URL with trailing slash")
        void urlWithTrailingSlash() {
            // The regex won't match a trailing slash case if the ID is followed by /
            // but will handle the clean case
            Optional<String> id = extractor.extractIdFromUrl(
                    "https://open.spotify.com/artist/6vWDO969PvNqNYHIOW5v0m/");
            // After the 22 chars, the / won't be captured — the regex still matches
            assertThat(id).contains("6vWDO969PvNqNYHIOW5v0m");
        }

        @Test
        @DisplayName("should return empty for non-artist Spotify URL")
        void nonArtistUrl() {
            Optional<String> id = extractor.extractIdFromUrl(
                    "https://open.spotify.com/track/6vWDO969PvNqNYHIOW5v0m");
            assertThat(id).isEmpty();
        }

        @Test
        @DisplayName("should return empty for null URL")
        void nullUrl() {
            assertThat(extractor.extractIdFromUrl(null)).isEmpty();
        }

        @Test
        @DisplayName("should return empty for blank URL")
        void blankUrl() {
            assertThat(extractor.extractIdFromUrl("")).isEmpty();
        }
    }

    @Nested
    @DisplayName("Ticketmaster data quality edge cases (from production)")
    class TicketmasterEdgeCases {

        @Test
        @DisplayName("should recover ID from mangled user/spotify:artist: URL (Luna)")
        void mangledSpotifyArtistUri() {
            // Real data: Luna's Spotify link in Ticketmaster
            Optional<String> id = extractor.extractIdFromUrl(
                    "https://open.spotify.com/user/spotify:artist:2AACqFGo8offvHCKGvrWxq");
            assertThat(id).contains("2AACqFGo8offvHCKGvrWxq");
        }

        @Test
        @DisplayName("should return empty for user profile URL (not an artist)")
        void userProfileUrl() {
            // Real data: Black Joe Lewis has a user URL, not artist URL
            assertThat(extractor.extractIdFromUrl(
                    "https://open.spotify.com/user/blackjoelewismusic")).isEmpty();
        }

        @Test
        @DisplayName("should return empty for playlist URL")
        void playlistUrl() {
            // Real data: Emo Night Brooklyn, Plies
            assertThat(extractor.extractIdFromUrl(
                    "https://open.spotify.com/playlist/3R4v3NMW8CbErUKNfCQbLm")).isEmpty();
            assertThat(extractor.extractIdFromUrl(
                    "https://open.spotify.com/playlist/3vwjBackAZ0Rl9hueMkOwp")).isEmpty();
        }

        @Test
        @DisplayName("should return empty for Apple Music URL in Spotify field")
        void appleMusicUrl() {
            // Real data: Microwave has an Apple Music URL in the Spotify field!
            assertThat(extractor.extractIdFromUrl(
                    "https://music.apple.com/us/artist/microwave/613522668")).isEmpty();
        }

        @Test
        @DisplayName("should return empty for completely unrelated homepage URL")
        void unrelatedUrl() {
            // Real data: Trap Karaoke has their homepage in the Spotify field
            assertThat(extractor.extractIdFromUrl("https://trapkaraoke.com/")).isEmpty();
        }

        @Test
        @DisplayName("should return empty for Yonder Mountain user URL")
        void yonderMountainUserUrl() {
            // Real data: Yonder Mountain String Band has a user URL
            assertThat(extractor.extractIdFromUrl(
                    "https://open.spotify.com/user/yondermountain")).isEmpty();
        }
    }

    @Nested
    @DisplayName("Attraction extraction")
    class AttractionExtraction {

        @Test
        @DisplayName("should extract Spotify ID from attraction with ExternalLink records")
        void fromAttraction_withSpotify() {
            AttractionResponse attraction = new AttractionResponse(
                    "K8vZ9171ob7", "Beyoncé", "en-us",
                    Map.of("spotify", List.of(
                            new ExternalLink("https://open.spotify.com/artist/6vWDO969PvNqNYHIOW5v0m", null)
                    )),
                    null, null
            );

            Optional<String> id = extractor.fromAttraction(attraction);
            assertThat(id).contains("6vWDO969PvNqNYHIOW5v0m");
        }

        @Test
        @DisplayName("should return empty when attraction has no spotify links")
        void fromAttraction_noSpotify() {
            AttractionResponse attraction = new AttractionResponse(
                    "K8vZ9171oZ7", "NY Knicks", "en-us",
                    Map.of("twitter", List.of(
                            new ExternalLink("https://twitter.com/nyknicks", null)
                    )),
                    null, null
            );

            assertThat(extractor.fromAttraction(attraction)).isEmpty();
        }

        @Test
        @DisplayName("should return empty when externalLinks is null")
        void fromAttraction_nullExternalLinks() {
            AttractionResponse attraction = new AttractionResponse(
                    "K8vZ9171oZ7", "Unknown Artist", "en-us",
                    null, null, null
            );

            assertThat(extractor.fromAttraction(attraction)).isEmpty();
        }

        @Test
        @DisplayName("should return empty for null attraction")
        void fromAttraction_null() {
            assertThat(extractor.fromAttraction(null)).isEmpty();
        }
    }

    @Nested
    @DisplayName("End-to-end with JSON fixtures")
    class FromJsonFixtures {

        @Test
        @DisplayName("should extract Beyoncé's Spotify ID from parsed JSON")
        void beyonce_fromJson() throws Exception {
            String json = loadFixture("sample-event-with-spotify.json");
            SearchResponse response = jsonMapper.readValue(json, SearchResponse.class);

            Optional<String> id = extractor.fromEvent(response.embedded().events().getFirst());
            assertThat(id).contains("6vWDO969PvNqNYHIOW5v0m");
            assertThat(extractor.isValidSpotifyId(id.get())).isTrue();
        }

        @Test
        @DisplayName("should extract Taylor Swift's Spotify ID from event without prices")
        void taylorSwift_fromJson() throws Exception {
            String json = loadFixture("sample-event-no-prices.json");
            SearchResponse response = jsonMapper.readValue(json, SearchResponse.class);

            Optional<String> id = extractor.fromEvent(response.embedded().events().getFirst());
            assertThat(id).contains("06HL4z0CvFAxyc27GXpf02");
        }

        @Test
        @DisplayName("should return empty for sports event without Spotify")
        void knicks_fromJson() throws Exception {
            String json = loadFixture("sample-event-no-spotify.json");
            SearchResponse response = jsonMapper.readValue(json, SearchResponse.class);

            Optional<String> id = extractor.fromEvent(response.embedded().events().getFirst());
            assertThat(id).isEmpty();
        }
    }

    @Nested
    @DisplayName("ID validation")
    class Validation {

        @Test
        @DisplayName("should validate correct 22-char base62 IDs")
        void validIds() {
            assertThat(extractor.isValidSpotifyId("6vWDO969PvNqNYHIOW5v0m")).isTrue();
            assertThat(extractor.isValidSpotifyId("06HL4z0CvFAxyc27GXpf02")).isTrue();
        }

        @Test
        @DisplayName("should reject invalid IDs")
        void invalidIds() {
            assertThat(extractor.isValidSpotifyId(null)).isFalse();
            assertThat(extractor.isValidSpotifyId("")).isFalse();
            assertThat(extractor.isValidSpotifyId("too-short")).isFalse();
            assertThat(extractor.isValidSpotifyId("has_special!chars@here")).isFalse();
        }
    }
}
