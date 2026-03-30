package com.mockhub.spotify.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.mockhub.spotify.entity.SpotifyListeningCache;

@Repository
public interface SpotifyListeningCacheRepository extends JpaRepository<SpotifyListeningCache, Long> {

    Optional<SpotifyListeningCache> findByUserId(Long userId);

    void deleteByUserId(Long userId);
}
