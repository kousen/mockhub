package com.mockhub;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import com.mockhub.auth.dto.AuthResponse;
import com.mockhub.auth.dto.UserDto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class AuthIntegrationTest extends AbstractIntegrationTest {

    @Test
    @DisplayName("Register, login, and access protected endpoint")
    void registerLoginAndAccessProtectedEndpoint() {
        // Register
        AuthResponse registerResponse = registerUser(
                "integration-auth@example.com", "password123", "Test", "User");
        assertNotNull(registerResponse, "Registration response should not be null");
        assertNotNull(registerResponse.accessToken(), "Access token should not be null");

        // Login
        AuthResponse loginResponse = loginUser("integration-auth@example.com", "password123");
        assertNotNull(loginResponse, "Login response should not be null");
        assertNotNull(loginResponse.accessToken(), "Login access token should not be null");

        // Access protected /me endpoint
        ResponseEntity<UserDto> meResponse = restTemplate.exchange(
                "/api/v1/auth/me",
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(loginResponse.accessToken())),
                UserDto.class);

        assertEquals(HttpStatus.OK, meResponse.getStatusCode(),
                "Accessing /me with valid token should return 200");
        assertNotNull(meResponse.getBody(), "User DTO should not be null");
        assertEquals("integration-auth@example.com", meResponse.getBody().email(),
                "Email should match registered email");
    }

    @Test
    @DisplayName("Access protected endpoint without token returns 401")
    void accessProtectedEndpointWithoutToken_returns401() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                "/api/v1/auth/me", String.class);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode(),
                "Accessing /me without token should return 401");
    }

    @Test
    @DisplayName("Register with duplicate email returns 409")
    void registerWithDuplicateEmail_returns409() {
        registerUser("dupe-auth@example.com", "password123", "First", "User");

        // Try to register again with same email
        String body = """
                {
                    "email": "dupe-auth@example.com",
                    "password": "password456",
                    "firstName": "Second",
                    "lastName": "User"
                }
                """;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/v1/auth/register",
                new HttpEntity<>(body, headers),
                String.class);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode(),
                "Duplicate registration should return 409");
    }
}
