package com.mockhub;

import com.mockhub.auth.dto.AuthResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RefreshTokenIntegrationTest extends AbstractIntegrationTest {

    private static final String PASSWORD = "SecurePass123!";

    @Test
    @DisplayName("Login sets refresh_token cookie with correct attributes")
    void login_setsRefreshCookieWithCorrectAttributes() {
        registerUser("refresh-test-1@example.com", PASSWORD, "Alice", "Test");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String body = """
                {"email":"refresh-test-1@example.com","password":"SecurePass123!"}""";

        ResponseEntity<AuthResponse> response = restTemplate.postForEntity(
                "/api/v1/auth/login", new HttpEntity<>(body, headers), AuthResponse.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());

        String setCookie = response.getHeaders().getFirst(HttpHeaders.SET_COOKIE);
        assertNotNull(setCookie, "Set-Cookie header must be present");
        assertTrue(setCookie.contains("refresh_token"), "Cookie must contain refresh_token");
        assertTrue(setCookie.contains("HttpOnly"), "Cookie must be HttpOnly");
        assertTrue(setCookie.contains("Path=/api/v1/auth/refresh"), "Cookie must have correct Path");
        assertTrue(setCookie.contains("Max-Age=604800"), "Cookie must have Max-Age=604800");
        assertTrue(setCookie.contains("SameSite=Lax"), "Cookie must have SameSite=Lax");
    }

    @Test
    @DisplayName("Refresh with valid cookie returns new access token")
    void refresh_givenValidCookie_returnsNewAccessToken() {
        registerUser("refresh-test-2@example.com", PASSWORD, "Bob", "Test");

        String cookieValue = loginAndExtractRefreshCookie("refresh-test-2@example.com");

        HttpHeaders headers = new HttpHeaders();
        headers.add("Cookie", "refresh_token=" + cookieValue);

        ResponseEntity<AuthResponse> response = restTemplate.postForEntity(
                "/api/v1/auth/refresh", new HttpEntity<>(null, headers), AuthResponse.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody(), "Response body must not be null");
        assertNotNull(response.getBody().accessToken(), "Access token must not be null");
    }

    @Test
    @DisplayName("Refresh without cookie returns 401")
    void refresh_givenNoCookie_returns401() {
        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/v1/auth/refresh", null, String.class);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    @DisplayName("Refresh with invalid token returns 401")
    void refresh_givenInvalidToken_returns401() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Cookie", "refresh_token=invalid.garbage.token");

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/v1/auth/refresh", new HttpEntity<>(null, headers), String.class);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    @DisplayName("New access token from refresh can access protected endpoint")
    void newAccessToken_givenFromRefresh_canAccessProtectedEndpoint() {
        registerUser("refresh-test-5@example.com", PASSWORD, "Eve", "Test");

        String cookieValue = loginAndExtractRefreshCookie("refresh-test-5@example.com");

        HttpHeaders refreshHeaders = new HttpHeaders();
        refreshHeaders.add("Cookie", "refresh_token=" + cookieValue);

        ResponseEntity<AuthResponse> refreshResponse = restTemplate.postForEntity(
                "/api/v1/auth/refresh", new HttpEntity<>(null, refreshHeaders), AuthResponse.class);

        assertEquals(HttpStatus.OK, refreshResponse.getStatusCode());
        assertNotNull(refreshResponse.getBody());
        String newAccessToken = refreshResponse.getBody().accessToken();
        assertNotNull(newAccessToken);

        HttpHeaders meHeaders = authHeaders(newAccessToken);
        ResponseEntity<String> meResponse = restTemplate.exchange(
                "/api/v1/auth/me", HttpMethod.GET, new HttpEntity<>(meHeaders), String.class);

        assertEquals(HttpStatus.OK, meResponse.getStatusCode());
    }

    /**
     * Logs in the given user and extracts the refresh_token value from the Set-Cookie header.
     */
    private String loginAndExtractRefreshCookie(String email) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String body = String.format("""
                {"email":"%s","password":"%s"}""", email, PASSWORD);

        ResponseEntity<AuthResponse> response = restTemplate.postForEntity(
                "/api/v1/auth/login", new HttpEntity<>(body, headers), AuthResponse.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());

        String setCookie = response.getHeaders().getFirst(HttpHeaders.SET_COOKIE);
        assertNotNull(setCookie, "Set-Cookie header must be present after login");

        // Set-Cookie format: refresh_token=<value>; Path=...; ...
        String[] parts = setCookie.split(";");
        String[] nameValue = parts[0].split("=", 2);
        assertEquals("refresh_token", nameValue[0].trim());
        return nameValue[1].trim();
    }
}
