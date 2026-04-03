package com.mockhub.auth.security;

import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.Cookie;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CookieOAuth2AuthorizationRequestRepositoryTest {

    private CookieOAuth2AuthorizationRequestRepository repository;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        repository = new CookieOAuth2AuthorizationRequestRepository(
                "dGVzdC1zZWNyZXQta2V5LWZvci1obWFjLXNpZ25pbmctdGVzdA==",
                false,
                new ObjectMapper());
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
    }

    private OAuth2AuthorizationRequest createTestRequest() {
        return OAuth2AuthorizationRequest.authorizationCode()
                .authorizationUri("https://accounts.google.com/o/oauth2/v2/auth")
                .clientId("test-client-id")
                .redirectUri("http://localhost:8080/login/oauth2/code/google")
                .state("test-state-abc123")
                .scopes(Set.of("openid", "email", "profile"))
                .build();
    }

    private OAuth2AuthorizationRequest createMinimalRequest() {
        return OAuth2AuthorizationRequest.authorizationCode()
                .authorizationUri("https://accounts.google.com/o/oauth2/v2/auth")
                .clientId("test-client-id")
                .build();
    }

    @Test
    @DisplayName("saveAuthorizationRequest - sets signed cookie")
    void saveAuthorizationRequest_setsSignedCookie() {
        OAuth2AuthorizationRequest authRequest = createTestRequest();

        repository.saveAuthorizationRequest(authRequest, request, response);

        Cookie cookie = response.getCookie(CookieOAuth2AuthorizationRequestRepository.COOKIE_NAME);
        assertNotNull(cookie);
        assertTrue(cookie.isHttpOnly());
        assertTrue(cookie.getValue().contains("."));
    }

    @Test
    @DisplayName("saveAuthorizationRequest - given null request - deletes cookie")
    void saveAuthorizationRequest_givenNull_deletesCookie() {
        repository.saveAuthorizationRequest(null, request, response);

        Cookie cookie = response.getCookie(CookieOAuth2AuthorizationRequestRepository.COOKIE_NAME);
        assertNotNull(cookie);
        assertEquals(0, cookie.getMaxAge());
    }

    @Test
    @DisplayName("loadAuthorizationRequest - round trip preserves data")
    void loadAuthorizationRequest_roundTripPreservesData() {
        OAuth2AuthorizationRequest original = createTestRequest();
        repository.saveAuthorizationRequest(original, request, response);

        Cookie savedCookie = response.getCookie(CookieOAuth2AuthorizationRequestRepository.COOKIE_NAME);
        request.setCookies(savedCookie);

        OAuth2AuthorizationRequest loaded = repository.loadAuthorizationRequest(request);

        assertNotNull(loaded);
        assertEquals(original.getAuthorizationUri(), loaded.getAuthorizationUri());
        assertEquals(original.getClientId(), loaded.getClientId());
        assertEquals(original.getRedirectUri(), loaded.getRedirectUri());
        assertEquals(original.getState(), loaded.getState());
    }

    @Test
    @DisplayName("loadAuthorizationRequest - given no cookies - returns null")
    void loadAuthorizationRequest_givenNoCookies_returnsNull() {
        OAuth2AuthorizationRequest result = repository.loadAuthorizationRequest(request);

        assertNull(result);
    }

    @Test
    @DisplayName("loadAuthorizationRequest - given tampered cookie - returns null")
    void loadAuthorizationRequest_givenTamperedCookie_returnsNull() {
        request.setCookies(new Cookie(
                CookieOAuth2AuthorizationRequestRepository.COOKIE_NAME,
                "dGFtcGVyZWQ.invalid-signature"));

        OAuth2AuthorizationRequest result = repository.loadAuthorizationRequest(request);

        assertNull(result);
    }

    @Test
    @DisplayName("loadAuthorizationRequest - given malformed cookie - returns null")
    void loadAuthorizationRequest_givenMalformedCookie_returnsNull() {
        request.setCookies(new Cookie(
                CookieOAuth2AuthorizationRequestRepository.COOKIE_NAME,
                "not-a-valid-format"));

        OAuth2AuthorizationRequest result = repository.loadAuthorizationRequest(request);

        assertNull(result);
    }

    @Test
    @DisplayName("loadAuthorizationRequest - given valid signature but invalid JSON - returns null")
    void loadAuthorizationRequest_givenValidSignatureInvalidJson_returnsNull() {
        // Create a cookie with valid HMAC signature but invalid JSON content
        String invalidJson = "not-valid-json{{{";
        String payload = java.util.Base64.getUrlEncoder().withoutPadding()
                .encodeToString(invalidJson.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        // Sign using the same key as the repository
        try {
            javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
            mac.init(new javax.crypto.spec.SecretKeySpec(
                    "dGVzdC1zZWNyZXQta2V5LWZvci1obWFjLXNpZ25pbmctdGVzdA=="
                            .getBytes(java.nio.charset.StandardCharsets.UTF_8),
                    "HmacSHA256"));
            byte[] sig = mac.doFinal(invalidJson.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            String signature = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(sig);

            request.setCookies(new Cookie(
                    CookieOAuth2AuthorizationRequestRepository.COOKIE_NAME,
                    payload + "." + signature));

            OAuth2AuthorizationRequest result = repository.loadAuthorizationRequest(request);
            assertNull(result);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("saveAuthorizationRequest - handles null redirectUri and state")
    void saveAuthorizationRequest_nullRedirectUriAndState_roundTrips() {
        OAuth2AuthorizationRequest authRequest = createMinimalRequest();

        repository.saveAuthorizationRequest(authRequest, request, response);

        Cookie savedCookie = response.getCookie(CookieOAuth2AuthorizationRequestRepository.COOKIE_NAME);
        assertNotNull(savedCookie);
        request.setCookies(savedCookie);

        OAuth2AuthorizationRequest loaded = repository.loadAuthorizationRequest(request);
        assertNotNull(loaded);
        assertNull(loaded.getRedirectUri());
        assertNull(loaded.getState());
    }

    @Test
    @DisplayName("loadAuthorizationRequest - ignores unrelated cookies")
    void loadAuthorizationRequest_unrelatedCookies_returnsNull() {
        request.setCookies(new Cookie("other_cookie", "some-value"));

        OAuth2AuthorizationRequest result = repository.loadAuthorizationRequest(request);

        assertNull(result);
    }

    @Test
    @DisplayName("removeAuthorizationRequest - returns request and clears cookie")
    void removeAuthorizationRequest_returnsRequestAndClearsCookie() {
        OAuth2AuthorizationRequest original = createTestRequest();
        repository.saveAuthorizationRequest(original, request, response);

        Cookie savedCookie = response.getCookie(CookieOAuth2AuthorizationRequestRepository.COOKIE_NAME);
        request.setCookies(savedCookie);

        MockHttpServletResponse removeResponse = new MockHttpServletResponse();
        OAuth2AuthorizationRequest removed = repository.removeAuthorizationRequest(request, removeResponse);

        assertNotNull(removed);
        assertEquals(original.getState(), removed.getState());

        Cookie deletedCookie = removeResponse.getCookie(CookieOAuth2AuthorizationRequestRepository.COOKIE_NAME);
        assertNotNull(deletedCookie);
        assertEquals(0, deletedCookie.getMaxAge());
    }
}
