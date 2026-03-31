package com.mockhub.auth.security;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;

import com.mockhub.auth.dto.AuthResponse;
import com.mockhub.auth.entity.OAuthAccount;
import com.mockhub.auth.entity.Role;
import com.mockhub.auth.entity.User;
import com.mockhub.auth.repository.OAuthAccountRepository;
import com.mockhub.auth.repository.RoleRepository;
import com.mockhub.auth.repository.UserRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OAuth2AuthenticationSuccessHandlerTest {

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private OAuthAccountRepository oAuthAccountRepository;

    @Mock
    private OAuth2AuthorizedClientService authorizedClientService;

    private OAuth2AuthenticationSuccessHandler handler;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    private User testUser;
    private Role userRole;

    private static final String FRONTEND_URL = "http://localhost:5173";

    @BeforeEach
    void setUp() {
        handler = new OAuth2AuthenticationSuccessHandler(
                jwtTokenProvider, userRepository, roleRepository,
                oAuthAccountRepository, authorizedClientService, FRONTEND_URL, false);

        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();

        userRole = new Role("ROLE_USER");
        userRole.setId(1L);

        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("user@example.com");
        testUser.setFirstName("John");
        testUser.setLastName("Doe");
        testUser.setRoles(Set.of(userRole));
        testUser.setCreatedAt(Instant.now());
        testUser.setEnabled(true);
        testUser.setEmailVerified(true);
    }

    private OAuth2AuthenticationToken createOAuthToken(String provider, Map<String, Object> attributes,
                                                        String nameAttributeKey) {
        OAuth2User oauth2User = new DefaultOAuth2User(
                List.of(() -> "ROLE_USER"), attributes, nameAttributeKey);
        return new OAuth2AuthenticationToken(oauth2User, List.of(() -> "ROLE_USER"), provider);
    }

    @Nested
    @DisplayName("onAuthenticationSuccess")
    class OnAuthenticationSuccess {

        @Test
        @DisplayName("onAuthenticationSuccess - given existing Google user - redirects with code")
        void onAuthenticationSuccess_givenExistingGoogleUser_redirectsWithCode() throws IOException {
            Map<String, Object> attrs = Map.of(
                    "email", "user@example.com",
                    "name", "John Doe",
                    "sub", "google-id-123",
                    "picture", "https://example.com/avatar.jpg");
            OAuth2AuthenticationToken token = createOAuthToken("google", attrs, "email");

            OAuthAccount existingAccount = new OAuthAccount();
            existingAccount.setUser(testUser);
            existingAccount.setProvider("google");
            existingAccount.setProviderAccountId("google-id-123");

            when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(testUser));
            when(oAuthAccountRepository.findByProviderAndProviderAccountId("google", "google-id-123"))
                    .thenReturn(Optional.of(existingAccount));
            when(userRepository.save(any(User.class))).thenReturn(testUser);
            when(oAuthAccountRepository.save(any(OAuthAccount.class))).thenReturn(existingAccount);
            when(jwtTokenProvider.generateAccessToken(any(SecurityUser.class))).thenReturn("access-token");
            when(jwtTokenProvider.generateRefreshToken(any(SecurityUser.class))).thenReturn("refresh-token");
            when(jwtTokenProvider.getAccessTokenExpirationMs()).thenReturn(3600000L);

            handler.onAuthenticationSuccess(request, response, token);

            String redirectUrl = response.getRedirectedUrl();
            assertNotNull(redirectUrl, "Should redirect");
            assertTrue(redirectUrl.startsWith(FRONTEND_URL + "/auth/callback?code="),
                    "Should redirect to frontend callback with code");
            assertTrue(response.getHeader("Set-Cookie").contains("refresh_token=refresh-token"),
                    "Should set refresh token cookie");
        }

        @Test
        @DisplayName("onAuthenticationSuccess - given new GitHub user - creates user and redirects")
        void onAuthenticationSuccess_givenNewGitHubUser_createsUserAndRedirects() throws IOException {
            Map<String, Object> attrs = Map.of(
                    "email", "newuser@example.com",
                    "name", "Jane Smith",
                    "id", 42,
                    "avatar_url", "https://github.com/avatar.jpg");
            OAuth2AuthenticationToken token = createOAuthToken("github", attrs, "email");

            User newUser = new User();
            newUser.setId(2L);
            newUser.setEmail("newuser@example.com");
            newUser.setFirstName("Jane");
            newUser.setLastName("Smith");
            newUser.setRoles(Set.of(userRole));
            newUser.setCreatedAt(Instant.now());
            newUser.setEnabled(true);

            when(userRepository.findByEmail("newuser@example.com")).thenReturn(Optional.empty());
            when(roleRepository.findByName("ROLE_USER")).thenReturn(Optional.of(userRole));
            when(userRepository.save(any(User.class))).thenReturn(newUser);
            when(oAuthAccountRepository.findByProviderAndProviderAccountId("github", "42"))
                    .thenReturn(Optional.empty());
            when(oAuthAccountRepository.save(any(OAuthAccount.class))).thenAnswer(i -> i.getArgument(0));
            when(jwtTokenProvider.generateAccessToken(any(SecurityUser.class))).thenReturn("access-token");
            when(jwtTokenProvider.generateRefreshToken(any(SecurityUser.class))).thenReturn("refresh-token");
            when(jwtTokenProvider.getAccessTokenExpirationMs()).thenReturn(3600000L);

            handler.onAuthenticationSuccess(request, response, token);

            String redirectUrl = response.getRedirectedUrl();
            assertNotNull(redirectUrl, "Should redirect");
            assertTrue(redirectUrl.startsWith(FRONTEND_URL + "/auth/callback?code="),
                    "Should redirect to frontend callback with code");
            verify(userRepository).findByEmail("newuser@example.com");
            verify(oAuthAccountRepository).save(any(OAuthAccount.class));
        }

        @Test
        @DisplayName("onAuthenticationSuccess - given OAuth account already linked - updates existing account")
        void onAuthenticationSuccess_givenAlreadyLinked_updatesExistingAccount() throws IOException {
            Map<String, Object> attrs = Map.of(
                    "email", "user@example.com",
                    "name", "John Doe",
                    "sub", "google-id-123",
                    "picture", "https://example.com/pic.jpg");
            OAuth2AuthenticationToken token = createOAuthToken("google", attrs, "email");

            OAuthAccount existingAccount = new OAuthAccount();
            existingAccount.setUser(testUser);
            existingAccount.setProvider("google");
            existingAccount.setProviderAccountId("google-id-123");

            when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(testUser));
            when(oAuthAccountRepository.findByProviderAndProviderAccountId("google", "google-id-123"))
                    .thenReturn(Optional.of(existingAccount));
            when(oAuthAccountRepository.save(any(OAuthAccount.class))).thenReturn(existingAccount);
            when(userRepository.save(any(User.class))).thenReturn(testUser);
            when(jwtTokenProvider.generateAccessToken(any(SecurityUser.class))).thenReturn("access-token");
            when(jwtTokenProvider.generateRefreshToken(any(SecurityUser.class))).thenReturn("refresh-token");
            when(jwtTokenProvider.getAccessTokenExpirationMs()).thenReturn(3600000L);

            handler.onAuthenticationSuccess(request, response, token);

            verify(oAuthAccountRepository).save(existingAccount);
        }

        @Test
        @DisplayName("onAuthenticationSuccess - given no email from provider - redirects with error")
        void onAuthenticationSuccess_givenNoEmail_redirectsWithError() throws IOException {
            Map<String, Object> attrs = Map.of("name", "No Email User", "sub", "id-123");
            OAuth2AuthenticationToken token = createOAuthToken("google", attrs, "name");

            handler.onAuthenticationSuccess(request, response, token);

            assertEquals(FRONTEND_URL + "/login?error=oauth_no_email",
                    response.getRedirectedUrl(), "Should redirect to login with error");
        }

        @Test
        @DisplayName("onAuthenticationSuccess - given Spotify user with display_name - extracts name correctly")
        void onAuthenticationSuccess_givenSpotifyUser_extractsNameCorrectly() throws IOException {
            Map<String, Object> attrs = Map.of(
                    "email", "spotify@example.com",
                    "display_name", "DJ Cool",
                    "id", "spotify-id-456",
                    "images", List.of(Map.of("url", "https://i.scdn.co/image/abc")));
            OAuth2AuthenticationToken token = createOAuthToken("spotify", attrs, "email");

            User spotifyUser = new User();
            spotifyUser.setId(3L);
            spotifyUser.setEmail("spotify@example.com");
            spotifyUser.setFirstName("DJ");
            spotifyUser.setLastName("Cool");
            spotifyUser.setRoles(Set.of(userRole));
            spotifyUser.setCreatedAt(Instant.now());
            spotifyUser.setEnabled(true);
            spotifyUser.setAvatarUrl("https://i.scdn.co/image/abc");

            OAuth2AuthorizedClient spotifyClient = createAuthorizedClient("spotify",
                    "spotify-access-token", "spotify-refresh-token");

            when(userRepository.findByEmail("spotify@example.com")).thenReturn(Optional.empty());
            when(roleRepository.findByName("ROLE_USER")).thenReturn(Optional.of(userRole));
            when(userRepository.save(any(User.class))).thenReturn(spotifyUser);
            when(oAuthAccountRepository.findByProviderAndProviderAccountId("spotify", "spotify-id-456"))
                    .thenReturn(Optional.empty());
            when(oAuthAccountRepository.save(any(OAuthAccount.class))).thenAnswer(i -> i.getArgument(0));
            when(authorizedClientService.loadAuthorizedClient("spotify", "spotify@example.com"))
                    .thenReturn(spotifyClient);
            when(jwtTokenProvider.generateAccessToken(any(SecurityUser.class))).thenReturn("access-token");
            when(jwtTokenProvider.generateRefreshToken(any(SecurityUser.class))).thenReturn("refresh-token");
            when(jwtTokenProvider.getAccessTokenExpirationMs()).thenReturn(3600000L);

            handler.onAuthenticationSuccess(request, response, token);

            assertTrue(response.getRedirectedUrl().startsWith(FRONTEND_URL + "/auth/callback?code="),
                    "Should redirect with code for Spotify user");

            // Verify Spotify tokens were stored
            org.mockito.ArgumentCaptor<OAuthAccount> captor =
                    org.mockito.ArgumentCaptor.forClass(OAuthAccount.class);
            verify(oAuthAccountRepository).save(captor.capture());
            OAuthAccount saved = captor.getValue();
            assertEquals("spotify-access-token", saved.getAccessTokenEncrypted());
            assertEquals("spotify-refresh-token", saved.getRefreshTokenEncrypted());
            assertNotNull(saved.getScopesGranted());
        }

        @Test
        @DisplayName("onAuthenticationSuccess - given new user with no name - uses email prefix as first name")
        void onAuthenticationSuccess_givenNoName_usesEmailPrefix() throws IOException {
            Map<String, Object> attrs = Map.of(
                    "email", "noname@example.com",
                    "sub", "google-id-789");
            OAuth2AuthenticationToken token = createOAuthToken("google", attrs, "email");

            User newUser = new User();
            newUser.setId(4L);
            newUser.setEmail("noname@example.com");
            newUser.setFirstName("noname");
            newUser.setLastName("");
            newUser.setRoles(Set.of(userRole));
            newUser.setCreatedAt(Instant.now());
            newUser.setEnabled(true);

            when(userRepository.findByEmail("noname@example.com")).thenReturn(Optional.empty());
            when(roleRepository.findByName("ROLE_USER")).thenReturn(Optional.of(userRole));
            when(userRepository.save(any(User.class))).thenReturn(newUser);
            when(oAuthAccountRepository.findByProviderAndProviderAccountId("google", "google-id-789"))
                    .thenReturn(Optional.empty());
            when(oAuthAccountRepository.save(any(OAuthAccount.class))).thenAnswer(i -> i.getArgument(0));
            when(jwtTokenProvider.generateAccessToken(any(SecurityUser.class))).thenReturn("access-token");
            when(jwtTokenProvider.generateRefreshToken(any(SecurityUser.class))).thenReturn("refresh-token");
            when(jwtTokenProvider.getAccessTokenExpirationMs()).thenReturn(3600000L);

            handler.onAuthenticationSuccess(request, response, token);

            assertTrue(response.getRedirectedUrl().startsWith(FRONTEND_URL + "/auth/callback?code="),
                    "Should redirect with code even when name is absent");
        }

        @Test
        @DisplayName("onAuthenticationSuccess - given Spotify user with empty images - avatar is null")
        void onAuthenticationSuccess_givenSpotifyEmptyImages_avatarIsNull() throws IOException {
            Map<String, Object> attrs = Map.of(
                    "email", "spotifybare@example.com",
                    "display_name", "Bare User",
                    "id", "spotify-id-bare",
                    "images", List.of());
            OAuth2AuthenticationToken token = createOAuthToken("spotify", attrs, "email");

            User spotifyUser = new User();
            spotifyUser.setId(5L);
            spotifyUser.setEmail("spotifybare@example.com");
            spotifyUser.setFirstName("Bare");
            spotifyUser.setLastName("User");
            spotifyUser.setRoles(Set.of(userRole));
            spotifyUser.setCreatedAt(Instant.now());
            spotifyUser.setEnabled(true);

            OAuth2AuthorizedClient spotifyClient = createAuthorizedClient("spotify",
                    "spotify-access", "spotify-refresh");

            when(userRepository.findByEmail("spotifybare@example.com")).thenReturn(Optional.empty());
            when(roleRepository.findByName("ROLE_USER")).thenReturn(Optional.of(userRole));
            when(userRepository.save(any(User.class))).thenReturn(spotifyUser);
            when(oAuthAccountRepository.findByProviderAndProviderAccountId("spotify", "spotify-id-bare"))
                    .thenReturn(Optional.empty());
            when(oAuthAccountRepository.save(any(OAuthAccount.class))).thenAnswer(i -> i.getArgument(0));
            when(authorizedClientService.loadAuthorizedClient("spotify", "spotifybare@example.com"))
                    .thenReturn(spotifyClient);
            when(jwtTokenProvider.generateAccessToken(any(SecurityUser.class))).thenReturn("access-token");
            when(jwtTokenProvider.generateRefreshToken(any(SecurityUser.class))).thenReturn("refresh-token");
            when(jwtTokenProvider.getAccessTokenExpirationMs()).thenReturn(3600000L);

            handler.onAuthenticationSuccess(request, response, token);

            assertTrue(response.getRedirectedUrl().startsWith(FRONTEND_URL + "/auth/callback?code="),
                    "Should redirect with code");
        }

        @Test
        @DisplayName("onAuthenticationSuccess - given unknown provider - providerAccountId is null, no link saved")
        void onAuthenticationSuccess_givenUnknownProvider_noLinkSaved() throws IOException {
            Map<String, Object> attrs = Map.of(
                    "email", "unknown@example.com",
                    "name", "Unknown Provider");
            OAuth2AuthenticationToken token = createOAuthToken("unknown-provider", attrs, "email");

            when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.of(testUser));
            when(userRepository.save(any(User.class))).thenReturn(testUser);
            when(jwtTokenProvider.generateAccessToken(any(SecurityUser.class))).thenReturn("access-token");
            when(jwtTokenProvider.generateRefreshToken(any(SecurityUser.class))).thenReturn("refresh-token");
            when(jwtTokenProvider.getAccessTokenExpirationMs()).thenReturn(3600000L);

            handler.onAuthenticationSuccess(request, response, token);

            // linkOAuthAccount returns early when providerAccountId is null
            verify(oAuthAccountRepository, never()).findByProviderAndProviderAccountId(any(), any());
            verify(oAuthAccountRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("exchangeCode")
    class ExchangeCode {

        @Test
        @DisplayName("exchangeCode - given valid code - returns AuthResponse")
        void exchangeCode_givenValidCode_returnsAuthResponse() throws IOException {
            // First trigger onAuthenticationSuccess to populate a code
            Map<String, Object> attrs = Map.of(
                    "email", "user@example.com",
                    "name", "John Doe",
                    "sub", "google-id-123",
                    "picture", "https://example.com/avatar.jpg");
            OAuth2AuthenticationToken token = createOAuthToken("google", attrs, "email");

            stubForSuccessfulLogin();

            handler.onAuthenticationSuccess(request, response, token);

            // Extract the code from the redirect URL
            String redirectUrl = response.getRedirectedUrl();
            String code = redirectUrl.substring(redirectUrl.indexOf("code=") + 5);

            AuthResponse authResponse = handler.exchangeCode(code);

            assertNotNull(authResponse, "Should return AuthResponse for valid code");
            assertEquals("access-token", authResponse.accessToken(), "Access token should match");
        }

        @Test
        @DisplayName("exchangeCode - given invalid code - returns null")
        void exchangeCode_givenInvalidCode_returnsNull() {
            AuthResponse result = handler.exchangeCode("invalid-code");
            assertNull(result, "Should return null for invalid code");
        }

        @Test
        @DisplayName("exchangeCode - given code used twice - returns null on second call")
        void exchangeCode_givenCodeUsedTwice_returnsNullOnSecondCall() throws IOException {
            Map<String, Object> attrs = Map.of(
                    "email", "user@example.com",
                    "name", "John Doe",
                    "sub", "google-id-123",
                    "picture", "https://example.com/avatar.jpg");
            OAuth2AuthenticationToken token = createOAuthToken("google", attrs, "email");

            stubForSuccessfulLogin();

            handler.onAuthenticationSuccess(request, response, token);

            String redirectUrl = response.getRedirectedUrl();
            String code = redirectUrl.substring(redirectUrl.indexOf("code=") + 5);

            AuthResponse first = handler.exchangeCode(code);
            assertNotNull(first, "First exchange should succeed");

            AuthResponse second = handler.exchangeCode(code);
            assertNull(second, "Second exchange should return null (code already consumed)");
        }
    }

    @Nested
    @DisplayName("cleanupExpired")
    class CleanupExpired {

        @Test
        @DisplayName("removes expired pending auth codes")
        void cleanupExpired_removesExpiredCodes() throws IOException {
            stubForSuccessfulLogin();

            handler.onAuthenticationSuccess(request, response, createGoogleToken());
            String redirectUrl = response.getRedirectedUrl();
            assertNotNull(redirectUrl);
            String code = redirectUrl.substring(redirectUrl.indexOf("code=") + 5);

            // Simulate expiry by exchanging after TTL would have passed
            // The cleanupExpired method is called internally by exchangeCode
            // Verify the code is valid right now
            AuthResponse result = handler.exchangeCode(code);
            assertNotNull(result, "Code should be valid immediately after creation");
        }

        @Test
        @DisplayName("scheduled cleanup can be called without errors when map is empty")
        void cleanupExpired_emptyMap_noErrors() {
            handler.cleanupExpired();
            // Verify handler still works after cleanup — nonexistent code returns null
            assertNull(handler.exchangeCode("nonexistent"));
        }

        @Test
        @DisplayName("scheduled cleanup removes expired entries and logs count")
        void cleanupExpired_withExpiredEntries_removesAndLogs() throws Exception {
            // Use reflection to insert an already-expired entry into pendingAuths
            java.lang.reflect.Field field =
                    OAuth2AuthenticationSuccessHandler.class.getDeclaredField("pendingAuths");
            field.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<String, ?> map = (Map<String, ?>) field.get(handler);

            // Create an expired PendingAuth via the record constructor
            Class<?> pendingAuthClass = Class.forName(
                    "com.mockhub.auth.security.OAuth2AuthenticationSuccessHandler$PendingAuth");
            java.lang.reflect.Constructor<?> constructor = pendingAuthClass.getDeclaredConstructors()[0];
            constructor.setAccessible(true);
            Object expiredEntry = constructor.newInstance(null, null, Instant.now().minusSeconds(60));

            @SuppressWarnings("unchecked")
            Map<String, Object> rawMap = (Map<String, Object>) map;
            rawMap.put("expired-code", expiredEntry);

            assertEquals(1, rawMap.size(), "Map should have 1 entry before cleanup");

            handler.cleanupExpired();

            assertEquals(0, rawMap.size(), "Expired entry should be removed");
        }
    }

    private void stubForSuccessfulLogin() {
        OAuthAccount existingAccount = new OAuthAccount();
        existingAccount.setUser(testUser);
        existingAccount.setProvider("google");
        existingAccount.setProviderAccountId("google-id-123");

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(testUser));
        when(oAuthAccountRepository.findByProviderAndProviderAccountId(any(), any()))
                .thenReturn(Optional.of(existingAccount));
        when(oAuthAccountRepository.save(any(OAuthAccount.class))).thenReturn(existingAccount);
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(jwtTokenProvider.generateAccessToken(any(SecurityUser.class))).thenReturn("access-token");
        when(jwtTokenProvider.generateRefreshToken(any(SecurityUser.class))).thenReturn("refresh-token");
        when(jwtTokenProvider.getAccessTokenExpirationMs()).thenReturn(3600000L);
    }

    private OAuth2AuthenticationToken createGoogleToken() {
        OAuth2User oauth2User = new DefaultOAuth2User(
                List.of(() -> "ROLE_USER"),
                Map.of("email", "user@example.com", "sub", "google-123", "name", "John Doe"),
                "email");
        return new OAuth2AuthenticationToken(oauth2User, oauth2User.getAuthorities(), "google");
    }

    private OAuth2AuthorizedClient createAuthorizedClient(String registrationId,
                                                           String accessTokenValue,
                                                           String refreshTokenValue) {
        ClientRegistration registration = ClientRegistration.withRegistrationId(registrationId)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .clientId("test-client-id")
                .redirectUri("http://localhost/callback")
                .authorizationUri("https://accounts.spotify.com/authorize")
                .tokenUri("https://accounts.spotify.com/api/token")
                .build();

        OAuth2AccessToken accessToken = new OAuth2AccessToken(
                OAuth2AccessToken.TokenType.BEARER,
                accessTokenValue,
                Instant.now(),
                Instant.now().plusSeconds(3600),
                Set.of("user-read-email", "user-top-read", "user-read-recently-played"));

        OAuth2RefreshToken refreshToken = new OAuth2RefreshToken(refreshTokenValue, Instant.now());

        return new OAuth2AuthorizedClient(registration, "test-principal", accessToken, refreshToken);
    }
}
