package com.mockhub.spotify.service;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.mockhub.auth.repository.OAuthAccountRepository;
import com.mockhub.spotify.entity.SpotifyListeningCache;
import com.mockhub.spotify.repository.SpotifyListeningCacheRepository;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SpotifyDataCleanupSchedulerTest {

    @Mock
    private SpotifyListeningCacheRepository cacheRepository;

    @Mock
    private OAuthAccountRepository oAuthAccountRepository;

    @InjectMocks
    private SpotifyDataCleanupScheduler scheduler;

    @Test
    @DisplayName("cleanupOrphanedCacheEntries - orphaned entry - deletes it")
    void cleanupOrphanedCacheEntries_orphanedEntry_deletesIt() {
        SpotifyListeningCache orphaned = createCache(1L, 42L);

        when(cacheRepository.findAll()).thenReturn(List.of(orphaned));
        when(oAuthAccountRepository.existsByUserIdAndProvider(42L, "spotify")).thenReturn(false);

        scheduler.cleanupOrphanedCacheEntries();

        verify(cacheRepository).delete(orphaned);
    }

    @Test
    @DisplayName("cleanupOrphanedCacheEntries - active entry - keeps it")
    void cleanupOrphanedCacheEntries_activeEntry_keepsIt() {
        SpotifyListeningCache active = createCache(1L, 42L);

        when(cacheRepository.findAll()).thenReturn(List.of(active));
        when(oAuthAccountRepository.existsByUserIdAndProvider(42L, "spotify")).thenReturn(true);

        scheduler.cleanupOrphanedCacheEntries();

        verify(cacheRepository, never()).delete(active);
    }

    @Test
    @DisplayName("cleanupOrphanedCacheEntries - empty table - no errors")
    void cleanupOrphanedCacheEntries_emptyTable_noErrors() {
        when(cacheRepository.findAll()).thenReturn(List.of());

        scheduler.cleanupOrphanedCacheEntries();
    }

    private SpotifyListeningCache createCache(Long id, Long userId) {
        SpotifyListeningCache cache = new SpotifyListeningCache();
        cache.setId(id);
        cache.setUserId(userId);
        cache.setTopArtistIds(List.of());
        cache.setTopArtistNames(List.of());
        cache.setTopGenres(List.of());
        cache.setRecentlyPlayedArtistIds(List.of());
        cache.setFetchedAt(Instant.now());
        return cache;
    }
}
