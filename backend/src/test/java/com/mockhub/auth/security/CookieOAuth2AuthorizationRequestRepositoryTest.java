package com.mockhub.auth.security;

import java.util.Base64;

import jakarta.servlet.http.Cookie;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.util.SerializationUtils;

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
        repository = new CookieOAuth2AuthorizationRequestRepository();
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
    }

    private OAuth2AuthorizationRequest createAuthorizationRequest() {
        return OAuth2AuthorizationRequest.authorizationCode()
                .clientId("test-client-id")
                .authorizationUri("https://accounts.google.com/o/oauth2/v2/auth")
                .redirectUri("http://localhost:8080/login/oauth2/code/google")
                .scopes(java.util.Set.of("openid", "email", "profile"))
                .state("random-state-value")
                .build();
    }

    @Test
    @DisplayName("saveAuthorizationRequest - given valid request - sets cookie with serialized data")
    void saveAuthorizationRequest_givenValidRequest_setsCookie() {
        OAuth2AuthorizationRequest authRequest = createAuthorizationRequest();

        repository.saveAuthorizationRequest(authRequest, request, response);

        Cookie cookie = response.getCookie(CookieOAuth2AuthorizationRequestRepository.COOKIE_NAME);
        assertNotNull(cookie, "Cookie should be set");
        assertEquals("/", cookie.getPath(), "Cookie path should be /");
        assertTrue(cookie.isHttpOnly(), "Cookie should be HttpOnly");
        assertEquals(600, cookie.getMaxAge(), "Cookie max age should be 600 seconds");
        assertNotNull(cookie.getValue(), "Cookie value should not be null");
    }

    @Test
    @DisplayName("saveAuthorizationRequest - given null request - deletes cookie")
    void saveAuthorizationRequest_givenNullRequest_deletesCookie() {
        repository.saveAuthorizationRequest(null, request, response);

        Cookie cookie = response.getCookie(CookieOAuth2AuthorizationRequestRepository.COOKIE_NAME);
        assertNotNull(cookie, "Delete cookie should be set");
        assertEquals(0, cookie.getMaxAge(), "Cookie max age should be 0 to delete it");
        assertEquals("", cookie.getValue(), "Cookie value should be empty");
    }

    @Test
    @DisplayName("loadAuthorizationRequest - given valid cookie - returns deserialized request")
    void loadAuthorizationRequest_givenValidCookie_returnsDeserializedRequest() {
        OAuth2AuthorizationRequest authRequest = createAuthorizationRequest();
        String encoded = Base64.getEncoder().encodeToString(
                SerializationUtils.serialize(authRequest));

        request.setCookies(new Cookie(CookieOAuth2AuthorizationRequestRepository.COOKIE_NAME, encoded));

        OAuth2AuthorizationRequest loaded = repository.loadAuthorizationRequest(request);

        assertNotNull(loaded, "Should deserialize authorization request from cookie");
        assertEquals("test-client-id", loaded.getClientId(), "Client ID should match");
        assertEquals("random-state-value", loaded.getState(), "State should match");
    }

    @Test
    @DisplayName("loadAuthorizationRequest - given no cookies - returns null")
    void loadAuthorizationRequest_givenNoCookies_returnsNull() {
        OAuth2AuthorizationRequest loaded = repository.loadAuthorizationRequest(request);

        assertNull(loaded, "Should return null when no cookies present");
    }

    @Test
    @DisplayName("loadAuthorizationRequest - given wrong cookie name - returns null")
    void loadAuthorizationRequest_givenWrongCookieName_returnsNull() {
        request.setCookies(new Cookie("wrong_name", "some-value"));

        OAuth2AuthorizationRequest loaded = repository.loadAuthorizationRequest(request);

        assertNull(loaded, "Should return null when cookie name does not match");
    }

    @Test
    @DisplayName("loadAuthorizationRequest - given corrupt cookie value - returns null")
    void loadAuthorizationRequest_givenCorruptCookieValue_returnsNull() {
        request.setCookies(new Cookie(
                CookieOAuth2AuthorizationRequestRepository.COOKIE_NAME, "not-valid-base64!!!"));

        OAuth2AuthorizationRequest loaded = repository.loadAuthorizationRequest(request);

        assertNull(loaded, "Should return null for corrupt cookie data");
    }

    @Test
    @DisplayName("removeAuthorizationRequest - given existing cookie - returns request and deletes cookie")
    void removeAuthorizationRequest_givenExistingCookie_returnsRequestAndDeletes() {
        OAuth2AuthorizationRequest authRequest = createAuthorizationRequest();
        String encoded = Base64.getEncoder().encodeToString(
                SerializationUtils.serialize(authRequest));

        request.setCookies(new Cookie(CookieOAuth2AuthorizationRequestRepository.COOKIE_NAME, encoded));

        OAuth2AuthorizationRequest removed = repository.removeAuthorizationRequest(request, response);

        assertNotNull(removed, "Should return the authorization request");
        assertEquals("test-client-id", removed.getClientId(), "Client ID should match");

        Cookie deleteCookie = response.getCookie(CookieOAuth2AuthorizationRequestRepository.COOKIE_NAME);
        assertNotNull(deleteCookie, "Should set a delete cookie");
        assertEquals(0, deleteCookie.getMaxAge(), "Delete cookie max age should be 0");
    }

    @Test
    @DisplayName("removeAuthorizationRequest - given no cookie - returns null and deletes cookie")
    void removeAuthorizationRequest_givenNoCookie_returnsNullAndDeletes() {
        OAuth2AuthorizationRequest removed = repository.removeAuthorizationRequest(request, response);

        assertNull(removed, "Should return null when no cookie present");

        Cookie deleteCookie = response.getCookie(CookieOAuth2AuthorizationRequestRepository.COOKIE_NAME);
        assertNotNull(deleteCookie, "Should still set a delete cookie");
        assertEquals(0, deleteCookie.getMaxAge(), "Delete cookie max age should be 0");
    }

    @Test
    @DisplayName("saveAuthorizationRequest then loadAuthorizationRequest - round trip preserves data")
    void saveAndLoad_roundTrip_preservesData() {
        OAuth2AuthorizationRequest authRequest = createAuthorizationRequest();

        repository.saveAuthorizationRequest(authRequest, request, response);

        // Extract the cookie from the response and add it to a new request
        Cookie savedCookie = response.getCookie(CookieOAuth2AuthorizationRequestRepository.COOKIE_NAME);
        assertNotNull(savedCookie, "Cookie should be set after save");

        MockHttpServletRequest newRequest = new MockHttpServletRequest();
        newRequest.setCookies(savedCookie);

        OAuth2AuthorizationRequest loaded = repository.loadAuthorizationRequest(newRequest);

        assertNotNull(loaded, "Should load saved authorization request");
        assertEquals(authRequest.getClientId(), loaded.getClientId(), "Client ID should round-trip");
        assertEquals(authRequest.getAuthorizationUri(), loaded.getAuthorizationUri(),
                "Authorization URI should round-trip");
        assertEquals(authRequest.getRedirectUri(), loaded.getRedirectUri(),
                "Redirect URI should round-trip");
        assertEquals(authRequest.getState(), loaded.getState(), "State should round-trip");
    }
}
