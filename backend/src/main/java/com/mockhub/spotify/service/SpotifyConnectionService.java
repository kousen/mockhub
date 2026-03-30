package com.mockhub.spotify.service;

import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mockhub.auth.entity.OAuthAccount;
import com.mockhub.auth.repository.OAuthAccountRepository;
import com.mockhub.spotify.dto.SpotifyConnectionDto;
import com.mockhub.spotify.repository.SpotifyListeningCacheRepository;

@Service
public class SpotifyConnectionService {

    private static final Logger log = LoggerFactory.getLogger(SpotifyConnectionService.class);
    private static final String PROVIDER_SPOTIFY = "spotify";
    private static final List<String> REQUIRED_SCOPES = List.of("user-top-read", "user-read-recently-played");

    private final OAuthAccountRepository oAuthAccountRepository;
    private final SpotifyListeningCacheRepository cacheRepository;

    public SpotifyConnectionService(OAuthAccountRepository oAuthAccountRepository,
                                     SpotifyListeningCacheRepository cacheRepository) {
        this.oAuthAccountRepository = oAuthAccountRepository;
        this.cacheRepository = cacheRepository;
    }

    @Transactional(readOnly = true)
    public SpotifyConnectionDto getConnectionStatus(Long userId) {
        Optional<OAuthAccount> accountOpt = oAuthAccountRepository.findByUserIdAndProvider(userId, PROVIDER_SPOTIFY);
        if (accountOpt.isEmpty()) {
            return SpotifyConnectionDto.notConnected();
        }

        OAuthAccount account = accountOpt.get();
        boolean needsUpgrade = account.getScopesGranted() == null
                || !REQUIRED_SCOPES.stream().allMatch(account.getScopesGranted()::contains);

        return new SpotifyConnectionDto(
                true,
                needsUpgrade,
                account.getProviderAccountId(),
                account.getCreatedAt());
    }

    @Transactional
    public void disconnect(Long userId) {
        oAuthAccountRepository.deleteByUserIdAndProvider(userId, PROVIDER_SPOTIFY);
        cacheRepository.deleteByUserId(userId);
        log.info("Disconnected Spotify for user ID {} — tokens and listening cache deleted",
                String.valueOf(userId).replaceAll("[\r\n]", ""));
    }
}
