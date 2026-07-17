package com.mockhub.spotify.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mockhub.spotify.entity.SpotifyListeningCache;

public interface SpotifyListeningCacheRepository extends JpaRepository<SpotifyListeningCache, Long> {

    Optional<SpotifyListeningCache> findByUserId(Long userId);

    void deleteByUserId(Long userId);
}
