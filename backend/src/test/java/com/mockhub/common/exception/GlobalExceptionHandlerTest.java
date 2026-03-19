package com.mockhub.common.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.mockhub.common.dto.ErrorResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    @DisplayName("handleDomainException - given ResourceNotFoundException - returns 404")
    void handleDomainException_givenResourceNotFoundException_returns404() {
        ResourceNotFoundException ex = new ResourceNotFoundException("Event", "id", 42L);

        ResponseEntity<ErrorResponse> response = handler.handleDomainException(ex);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(404, response.getBody().status());
        assertEquals("Not Found", response.getBody().error());
        assertEquals("Event not found with id: '42'", response.getBody().message());
    }

    @Test
    @DisplayName("handleDomainException - given ConflictException - returns 409")
    void handleDomainException_givenConflictException_returns409() {
        ConflictException ex = new ConflictException("Email already registered");

        ResponseEntity<ErrorResponse> response = handler.handleDomainException(ex);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(409, response.getBody().status());
        assertEquals("Conflict", response.getBody().error());
        assertEquals("Email already registered", response.getBody().message());
    }

    @Test
    @DisplayName("handleDomainException - given PaymentException - returns 402")
    void handleDomainException_givenPaymentException_returns402() {
        PaymentException ex = new PaymentException("Card declined");

        ResponseEntity<ErrorResponse> response = handler.handleDomainException(ex);

        assertEquals(HttpStatus.PAYMENT_REQUIRED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(402, response.getBody().status());
        assertEquals("Payment Required", response.getBody().error());
        assertEquals("Card declined", response.getBody().message());
    }

    @Test
    @DisplayName("handleDomainException - given UnauthorizedException - returns 401")
    void handleDomainException_givenUnauthorizedException_returns401() {
        UnauthorizedException ex = new UnauthorizedException("Invalid credentials");

        ResponseEntity<ErrorResponse> response = handler.handleDomainException(ex);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(401, response.getBody().status());
        assertEquals("Unauthorized", response.getBody().error());
        assertEquals("Invalid credentials", response.getBody().message());
    }
}
