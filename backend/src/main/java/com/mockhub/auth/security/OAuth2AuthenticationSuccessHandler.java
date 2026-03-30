package com.mockhub.auth.security;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.mockhub.auth.dto.AuthResponse;
import com.mockhub.auth.dto.UserDto;
import com.mockhub.auth.entity.OAuthAccount;
import com.mockhub.auth.entity.Role;
import com.mockhub.auth.entity.User;
import com.mockhub.auth.repository.OAuthAccountRepository;
import com.mockhub.auth.repository.RoleRepository;
import com.mockhub.auth.repository.UserRepository;

@Component
public class OAuth2AuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private static final Logger log = LoggerFactory.getLogger(OAuth2AuthenticationSuccessHandler.class);
    private static final long CODE_TTL_MS = 30_000;
    private static final String PROVIDER_GOOGLE = "google";
    private static final String PROVIDER_GITHUB = "github";
    private static final String PROVIDER_SPOTIFY = "spotify";

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final OAuthAccountRepository oAuthAccountRepository;
    private final OAuth2AuthorizedClientService authorizedClientService;
    private final String frontendRedirectUrl;
    private final boolean secureCookie;

    private final Map<String, PendingAuth> pendingAuths = new ConcurrentHashMap<>();

    public OAuth2AuthenticationSuccessHandler(
            JwtTokenProvider jwtTokenProvider,
            UserRepository userRepository,
            RoleRepository roleRepository,
            OAuthAccountRepository oAuthAccountRepository,
            OAuth2AuthorizedClientService authorizedClientService,
            @Value("${mockhub.oauth2.frontend-redirect-url}") String frontendRedirectUrl,
            @Value("${mockhub.cookie.secure:false}") boolean secureCookie) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.oAuthAccountRepository = oAuthAccountRepository;
        this.authorizedClientService = authorizedClientService;
        this.frontendRedirectUrl = frontendRedirectUrl;
        this.secureCookie = secureCookie;
    }

    @Override
    @Transactional
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
        OAuth2User oauth2User = oauthToken.getPrincipal();
        String provider = oauthToken.getAuthorizedClientRegistrationId();

        String email = extractEmail(oauth2User);
        if (email == null) {
            log.error("Could not extract email from {} OAuth2 user", provider);
            response.sendRedirect(frontendRedirectUrl + "/login?error=oauth_no_email");
            return;
        }

        String providerAccountId = extractProviderAccountId(oauth2User, provider);
        String name = extractName(oauth2User, provider);
        String avatarUrl = extractAvatarUrl(oauth2User, provider);

        User user = findOrCreateUser(email, name, avatarUrl);

        OAuth2AuthorizedClient authorizedClient = authorizedClientService.loadAuthorizedClient(
                provider, oauthToken.getName());
        linkOAuthAccount(user, provider, providerAccountId, authorizedClient);

        user.setLastLoginAt(Instant.now());
        userRepository.save(user);

        SecurityUser securityUser = new SecurityUser(user);
        String accessToken = jwtTokenProvider.generateAccessToken(securityUser);
        String refreshToken = jwtTokenProvider.generateRefreshToken(securityUser);
        long expiresIn = jwtTokenProvider.getAccessTokenExpirationMs() / 1000;
        UserDto userDto = toUserDto(user);

        AuthResponse authResponse = new AuthResponse(accessToken, expiresIn, userDto);

        String code = UUID.randomUUID().toString();
        pendingAuths.put(code, new PendingAuth(authResponse, refreshToken,
                Instant.now().plusMillis(CODE_TTL_MS)));
        cleanupExpired();

        ResponseCookie refreshCookie = ResponseCookie.from("refresh_token", refreshToken)
                .httpOnly(true)
                .secure(secureCookie)
                .path("/api/v1/auth/refresh")
                .maxAge(Duration.ofDays(7))
                .sameSite("Lax")
                .build();
        response.addHeader("Set-Cookie", refreshCookie.toString());

        log.info("OAuth2 login successful for {} via {}", email, provider);
        response.sendRedirect(frontendRedirectUrl + "/auth/callback?code=" + code);
    }

    public AuthResponse exchangeCode(String code) {
        cleanupExpired();
        PendingAuth pending = pendingAuths.remove(code);
        if (pending == null || Instant.now().isAfter(pending.expiry())) {
            return null;
        }
        return pending.authResponse();
    }

    private User findOrCreateUser(String email, String name, String avatarUrl) {
        return userRepository.findByEmail(email).orElseGet(() -> {
            Role userRole = roleRepository.findByName("ROLE_USER")
                    .orElseThrow(() -> new IllegalStateException("ROLE_USER not found"));

            User newUser = new User();
            newUser.setEmail(email);
            newUser.setPasswordHash(null);

            String[] parts = (name != null ? name : email.split("@")[0]).split(" ", 2);
            newUser.setFirstName(parts[0]);
            newUser.setLastName(parts.length > 1 ? parts[1] : "");

            newUser.setAvatarUrl(avatarUrl);
            newUser.setEmailVerified(true);
            newUser.setEnabled(true);
            newUser.setRoles(Set.of(userRole));

            User saved = userRepository.save(newUser);
            log.info("Created new OAuth2 user: {}", email);
            return saved;
        });
    }

    private void linkOAuthAccount(User user, String provider, String providerAccountId,
                                   OAuth2AuthorizedClient authorizedClient) {
        if (providerAccountId == null) {
            return;
        }
        OAuthAccount account = oAuthAccountRepository
                .findByProviderAndProviderAccountId(provider, providerAccountId)
                .orElse(null);

        if (account == null) {
            account = new OAuthAccount();
            account.setUser(user);
            account.setProvider(provider);
            account.setProviderAccountId(providerAccountId);
        }

        if (PROVIDER_SPOTIFY.equals(provider) && authorizedClient != null) {
            storeSpotifyTokens(account, authorizedClient);
        }

        oAuthAccountRepository.save(account);
        log.info("Linked {} account to user {}", provider, user.getEmail());
    }

    private void storeSpotifyTokens(OAuthAccount account, OAuth2AuthorizedClient authorizedClient) {
        OAuth2AccessToken accessToken = authorizedClient.getAccessToken();
        OAuth2RefreshToken refreshToken = authorizedClient.getRefreshToken();

        account.setAccessTokenEncrypted(accessToken.getTokenValue());
        if (accessToken.getExpiresAt() != null) {
            account.setTokenExpiresAt(accessToken.getExpiresAt());
        }
        if (refreshToken != null) {
            account.setRefreshTokenEncrypted(refreshToken.getTokenValue());
        }

        Set<String> scopes = accessToken.getScopes();
        if (scopes != null && !scopes.isEmpty()) {
            account.setScopesGranted(String.join(",", scopes));
        }

        log.info("Stored Spotify tokens for user {} (scopes: {})",
                account.getUser().getEmail(), account.getScopesGranted());
    }

    private String extractEmail(OAuth2User user) {
        return user.getAttribute("email");
    }

    private String extractProviderAccountId(OAuth2User user, String provider) {
        return switch (provider) {
            case PROVIDER_GOOGLE -> user.getAttribute("sub");
            case PROVIDER_GITHUB -> String.valueOf((Object) user.getAttribute("id"));
            case PROVIDER_SPOTIFY -> user.getAttribute("id");
            default -> null;
        };
    }

    private String extractName(OAuth2User user, String provider) {
        return switch (provider) {
            case PROVIDER_SPOTIFY -> user.getAttribute("display_name");
            default -> user.getAttribute("name");
        };
    }

    @SuppressWarnings("unchecked")
    private String extractAvatarUrl(OAuth2User user, String provider) {
        return switch (provider) {
            case PROVIDER_GOOGLE -> user.getAttribute("picture");
            case PROVIDER_GITHUB -> user.getAttribute("avatar_url");
            case PROVIDER_SPOTIFY -> {
                Object images = user.getAttribute("images");
                if (images instanceof List<?> imageList && !imageList.isEmpty()) {
                    Object first = imageList.getFirst();
                    if (first instanceof Map<?, ?> imageMap) {
                        yield (String) imageMap.get("url");
                    }
                }
                yield null;
            }
            default -> null;
        };
    }

    private UserDto toUserDto(User user) {
        java.util.Set<String> roles = user.getRoles().stream()
                .map(Role::getName)
                .collect(java.util.stream.Collectors.toSet());
        return new UserDto(
                user.getId(), user.getEmail(), user.getFirstName(), user.getLastName(),
                user.getPhone(), user.getAvatarUrl(), user.isEmailVerified(),
                roles, user.getCreatedAt());
    }

    @org.springframework.scheduling.annotation.Scheduled(fixedRateString = "${mockhub.oauth2.cleanup-interval-ms:300000}")
    void cleanupExpired() {
        int sizeBefore = pendingAuths.size();
        Instant now = Instant.now();
        pendingAuths.entrySet().removeIf(entry -> now.isAfter(entry.getValue().expiry()));
        int removed = sizeBefore - pendingAuths.size();
        if (removed > 0) {
            log.info("Cleaned up {} expired OAuth pending auth codes", removed);
        }
    }

    record PendingAuth(AuthResponse authResponse, String refreshToken, Instant expiry) {
    }
}
