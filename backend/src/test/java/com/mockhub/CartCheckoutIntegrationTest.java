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

class CartCheckoutIntegrationTest extends AbstractIntegrationTest {

    @Test
    @DisplayName("Authenticated user can access cart endpoint")
    void authenticatedUser_canAccessCartEndpoint() {
        AuthResponse auth = registerUser(
                "cart-user@example.com", "password123", "Cart", "User");
        assertNotNull(auth, "Auth response should not be null");

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/cart",
                HttpMethod.GET,
                new HttpEntity<>(authHeaders(auth.accessToken())),
                String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode(),
                "Authenticated user should access cart endpoint");
    }

    @Test
    @DisplayName("Unauthenticated user cannot access cart endpoint")
    void unauthenticatedUser_cannotAccessCartEndpoint() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                "/api/v1/cart", String.class);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode(),
                "Unauthenticated user should get 401 on cart endpoint");
    }

    @Test
    @DisplayName("Checkout with empty cart returns conflict")
    void checkoutWithEmptyCart_returnsConflict() {
        AuthResponse auth = registerUser(
                "checkout-empty@example.com", "password123", "Checkout", "User");
        assertNotNull(auth, "Auth response should not be null");

        String body = """
                {"paymentMethod": "mock"}
                """;

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/v1/orders/checkout",
                HttpMethod.POST,
                new HttpEntity<>(body, authHeaders(auth.accessToken())),
                String.class);

        // Should fail because cart is empty or doesn't exist
        assertEquals(HttpStatus.CONFLICT, response.getStatusCode(),
                "Checkout with empty/no cart should return 409");
    }
}
