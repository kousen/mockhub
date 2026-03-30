package com.mockhub.spotify.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.mockhub.auth.repository.OAuthAccountRepository;
import com.mockhub.spotify.entity.SpotifyListeningCache;
import com.mockhub.spotify.repository.SpotifyListeningCacheRepository;

/**
 * Nightly cleanup of orphaned Spotify listening cache entries.
 * Removes cache rows where the user no longer has a linked Spotify account.
 */
@Component
public class SpotifyDataCleanupScheduler {

    private static final Logger log = LoggerFactory.getLogger(SpotifyDataCleanupScheduler.class);

    private final SpotifyListeningCacheRepository cacheRepository;
    private final OAuthAccountRepository oAuthAccountRepository;

    public SpotifyDataCleanupScheduler(SpotifyListeningCacheRepository cacheRepository,
                                        OAuthAccountRepository oAuthAccountRepository) {
        this.cacheRepository = cacheRepository;
        this.oAuthAccountRepository = oAuthAccountRepository;
    }

    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void cleanupOrphanedCacheEntries() {
        List<SpotifyListeningCache> allCacheEntries = cacheRepository.findAll();
        int removed = 0;

        for (SpotifyListeningCache cache : allCacheEntries) {
            boolean hasSpotifyAccount = oAuthAccountRepository.existsByUserIdAndProvider(
                    cache.getUserId(), "spotify");
            if (!hasSpotifyAccount) {
                cacheRepository.delete(cache);
                removed++;
            }
        }

        if (removed > 0) {
            log.info("Cleaned up {} orphaned Spotify listening cache entries", removed);
        }
    }
}
