package com.mockhub.spotify.entity;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "spotify_listening_cache")
public class SpotifyListeningCache {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(name = "top_artist_ids", columnDefinition = "jsonb", nullable = false)
    @JdbcTypeCode(SqlTypes.JSON)
    private List<String> topArtistIds = new ArrayList<>();

    @Column(name = "top_artist_names", columnDefinition = "jsonb", nullable = false)
    @JdbcTypeCode(SqlTypes.JSON)
    private List<String> topArtistNames = new ArrayList<>();

    @Column(name = "top_genres", columnDefinition = "jsonb", nullable = false)
    @JdbcTypeCode(SqlTypes.JSON)
    private List<String> topGenres = new ArrayList<>();

    @Column(name = "recently_played_artist_ids", columnDefinition = "jsonb", nullable = false)
    @JdbcTypeCode(SqlTypes.JSON)
    private List<String> recentlyPlayedArtistIds = new ArrayList<>();

    @Column(name = "fetched_at", nullable = false)
    private Instant fetchedAt;

    public SpotifyListeningCache() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public List<String> getTopArtistIds() {
        return topArtistIds;
    }

    public void setTopArtistIds(List<String> topArtistIds) {
        this.topArtistIds = topArtistIds;
    }

    public List<String> getTopArtistNames() {
        return topArtistNames;
    }

    public void setTopArtistNames(List<String> topArtistNames) {
        this.topArtistNames = topArtistNames;
    }

    public List<String> getTopGenres() {
        return topGenres;
    }

    public void setTopGenres(List<String> topGenres) {
        this.topGenres = topGenres;
    }

    public List<String> getRecentlyPlayedArtistIds() {
        return recentlyPlayedArtistIds;
    }

    public void setRecentlyPlayedArtistIds(List<String> recentlyPlayedArtistIds) {
        this.recentlyPlayedArtistIds = recentlyPlayedArtistIds;
    }

    public Instant getFetchedAt() {
        return fetchedAt;
    }

    public void setFetchedAt(Instant fetchedAt) {
        this.fetchedAt = fetchedAt;
    }
}
