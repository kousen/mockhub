package com.mockhub;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;

import com.mockhub.auth.dto.AuthResponse;
import com.mockhub.auth.entity.Role;
import com.mockhub.auth.entity.User;
import com.mockhub.auth.repository.OAuthAccountRepository;
import com.mockhub.auth.repository.RoleRepository;
import com.mockhub.auth.repository.UserRepository;
import com.mockhub.auth.security.OAuth2AuthenticationSuccessHandler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration test for the full OAuth2 login flow.
 * Uses the real OAuth2AuthenticationSuccessHandler with a simulated
 * provider callback, then exercises the code exchange endpoint.
 */
class OAuth2IntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private OAuth2AuthenticationSuccessHandler oauth2SuccessHandler;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OAuthAccountRepository oAuthAccountRepository;

    @Autowired
    private RoleRepository roleRepository;

    @org.junit.jupiter.api.BeforeEach
    void ensureRoleUserExists() {
        if (roleRepository.findByName("ROLE_USER").isEmpty()) {
            roleRepository.save(new Role("ROLE_USER"));
        }
    }

    @Test
    @DisplayName("OAuth2 flow creates user, links provider, and returns valid JWT on code exchange")
    void oauthFlow_createsUserAndLinksProvider() throws Exception {
        String testEmail = "oauth-test-" + System.currentTimeMillis() + "@example.com";

        // Simulate OAuth2 provider callback
        OAuth2User oauth2User = new DefaultOAuth2User(
                List.of(() -> "ROLE_USER"),
                Map.of("email", testEmail, "sub", "google-12345", "name", "OAuth Test User"),
                "email");
        OAuth2AuthenticationToken oauthToken =
                new OAuth2AuthenticationToken(oauth2User, oauth2User.getAuthorities(), "google");

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        oauth2SuccessHandler.onAuthenticationSuccess(request, response, oauthToken);

        // Extract the one-time code from the redirect URL
        String redirectUrl = response.getRedirectedUrl();
        assertNotNull(redirectUrl, "Should redirect after OAuth success");
        assertTrue(redirectUrl.contains("code="), "Redirect should contain code parameter");
        String code = redirectUrl.substring(redirectUrl.indexOf("code=") + 5);

        // Exchange the code via the REST endpoint
        ResponseEntity<AuthResponse> exchangeResponse = restTemplate.postForEntity(
                "/api/v1/auth/oauth2/exchange?code=" + code,
                null,
                AuthResponse.class);

        assertEquals(HttpStatus.OK, exchangeResponse.getStatusCode(),
                "Code exchange should return 200");
        AuthResponse authResponse = exchangeResponse.getBody();
        assertNotNull(authResponse, "Auth response should not be null");
        assertNotNull(authResponse.accessToken(), "Should return access token");
        assertEquals(testEmail, authResponse.user().email(), "User email should match");

        // Verify user was created in the database
        User user = userRepository.findByEmail(testEmail).orElse(null);
        assertNotNull(user, "User should exist in database");
        assertEquals("OAuth", user.getFirstName(), "First name should be first word of OAuth name");
        assertEquals("Test User", user.getLastName(), "Last name should be remainder of OAuth name");

        // Verify OAuth account was linked
        assertTrue(oAuthAccountRepository.existsByUserIdAndProvider(user.getId(), "google"),
                "Google provider should be linked");

        // Verify the JWT works for authenticated requests
        HttpHeaders headers = authHeaders(authResponse.accessToken());
        ResponseEntity<String> meResponse = restTemplate.exchange(
                "/api/v1/auth/me", HttpMethod.GET,
                new HttpEntity<>(headers), String.class);
        assertEquals(HttpStatus.OK, meResponse.getStatusCode(),
                "Authenticated request should succeed with OAuth JWT");
    }

    @Test
    @DisplayName("OAuth2 flow links provider to existing user when email matches")
    void oauthFlow_linksToExistingUser() throws Exception {
        // First, register a user with email/password
        String testEmail = "existing-oauth-" + System.currentTimeMillis() + "@example.com";
        AuthResponse registered = registerUser(testEmail, "password123", "Existing", "User");
        assertNotNull(registered, "Registration should succeed");

        // Now simulate OAuth login with the same email
        OAuth2User oauth2User = new DefaultOAuth2User(
                List.of(() -> "ROLE_USER"),
                Map.of("email", testEmail, "id", "github-67890", "name", "Existing User"),
                "email");
        OAuth2AuthenticationToken oauthToken =
                new OAuth2AuthenticationToken(oauth2User, oauth2User.getAuthorities(), "github");

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        oauth2SuccessHandler.onAuthenticationSuccess(request, response, oauthToken);

        String redirectUrl = response.getRedirectedUrl();
        String code = redirectUrl.substring(redirectUrl.indexOf("code=") + 5);

        ResponseEntity<AuthResponse> exchangeResponse = restTemplate.postForEntity(
                "/api/v1/auth/oauth2/exchange?code=" + code,
                null,
                AuthResponse.class);

        assertEquals(HttpStatus.OK, exchangeResponse.getStatusCode());

        // Verify the same user now has GitHub linked
        User user = userRepository.findByEmail(testEmail).orElse(null);
        assertNotNull(user);
        assertTrue(oAuthAccountRepository.existsByUserIdAndProvider(user.getId(), "github"),
                "GitHub provider should be linked to existing user");
    }

    @Test
    @DisplayName("Code exchange with invalid code returns 401")
    void codeExchange_invalidCode_returns401() {
        ResponseEntity<AuthResponse> response = restTemplate.postForEntity(
                "/api/v1/auth/oauth2/exchange?code=invalid-code",
                null,
                AuthResponse.class);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode(),
                "Invalid code should return 401");
    }

    @Test
    @DisplayName("Provider unlinking works via REST endpoint")
    void unlinkProvider_removesOAuthAccount() throws Exception {
        String testEmail = "unlink-test-" + System.currentTimeMillis() + "@example.com";

        // Register with password first
        AuthResponse registered = registerUser(testEmail, "password123", "Unlink", "Test");

        // Link a Google account via OAuth flow
        OAuth2User oauth2User = new DefaultOAuth2User(
                List.of(() -> "ROLE_USER"),
                Map.of("email", testEmail, "sub", "google-unlink-123", "name", "Unlink Test"),
                "email");
        OAuth2AuthenticationToken oauthToken =
                new OAuth2AuthenticationToken(oauth2User, oauth2User.getAuthorities(), "google");

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        oauth2SuccessHandler.onAuthenticationSuccess(request, response, oauthToken);

        User user = userRepository.findByEmail(testEmail).orElse(null);
        assertNotNull(user);
        assertTrue(oAuthAccountRepository.existsByUserIdAndProvider(user.getId(), "google"));

        // Unlink the provider
        HttpHeaders headers = authHeaders(registered.accessToken());
        ResponseEntity<Void> unlinkResponse = restTemplate.exchange(
                "/api/v1/auth/me/providers/google", HttpMethod.DELETE,
                new HttpEntity<>(headers), Void.class);

        assertEquals(HttpStatus.NO_CONTENT, unlinkResponse.getStatusCode(),
                "Unlink should return 204");
        assertFalse(oAuthAccountRepository.existsByUserIdAndProvider(user.getId(), "google"),
                "Google provider should be unlinked");
    }
}
