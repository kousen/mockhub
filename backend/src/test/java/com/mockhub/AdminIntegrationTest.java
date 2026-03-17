package com.mockhub;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.mockhub.auth.dto.AuthResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class AdminIntegrationTest extends AbstractIntegrationTest {

    @Test
    @DisplayName("Non-admin user cannot access admin endpoints")
    void nonAdminUser_cannotAccessAdminEndpoints() {
        AuthResponse auth = registerUser(
                "regular-user-admin@example.com", "password123", "Regular", "User");
        assertNotNull(auth, "Auth response should not be null");

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/admin/dashboard",
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(auth.accessToken())),
                String.class);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode(),
                "Non-admin user should get 403 on admin endpoints");
    }

    @Test
    @DisplayName("Unauthenticated user cannot access admin endpoints")
    void unauthenticatedUser_cannotAccessAdminEndpoints() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                "/api/v1/admin/dashboard", String.class);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode(),
                "Unauthenticated user should get 401 on admin endpoints");
    }

    @Test
    @DisplayName("Non-admin user cannot access admin events")
    void nonAdminUser_cannotAccessAdminEvents() {
        AuthResponse auth = registerUser(
                "regular-user-admin-events@example.com", "password123", "Regular", "User");
        assertNotNull(auth, "Auth response should not be null");

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/admin/events",
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(auth.accessToken())),
                String.class);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode(),
                "Non-admin user should get 403 on admin events");
    }

    @Test
    @DisplayName("Non-admin user cannot access admin users")
    void nonAdminUser_cannotAccessAdminUsers() {
        AuthResponse auth = registerUser(
                "regular-user-admin-users@example.com", "password123", "Regular", "User");
        assertNotNull(auth, "Auth response should not be null");

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/admin/users",
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(auth.accessToken())),
                String.class);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode(),
                "Non-admin user should get 403 on admin users");
    }
}
