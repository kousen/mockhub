package com.mockhub.common.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.context.request.WebRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    @DisplayName("handleDomainException - given ResourceNotFoundException - returns 404 ProblemDetail")
    void handleDomainException_givenResourceNotFoundException_returns404() {
        ResourceNotFoundException ex = new ResourceNotFoundException("Event", "id", 42L);

        ResponseEntity<ProblemDetail> response = handler.handleDomainException(ex);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        ProblemDetail body = response.getBody();
        assertNotNull(body);
        assertEquals(404, body.getStatus());
        assertEquals("Not Found", body.getTitle());
        assertEquals("Event not found with id: '42'", body.getDetail());
    }

    @Test
    @DisplayName("handleDomainException - given ConflictException - returns 409 ProblemDetail")
    void handleDomainException_givenConflictException_returns409() {
        ConflictException ex = new ConflictException("Email already registered");

        ResponseEntity<ProblemDetail> response = handler.handleDomainException(ex);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        ProblemDetail body = response.getBody();
        assertNotNull(body);
        assertEquals(409, body.getStatus());
        assertEquals("Conflict", body.getTitle());
        assertEquals("Email already registered", body.getDetail());
    }

    @Test
    @DisplayName("handleDomainException - given PaymentException - returns 402 ProblemDetail")
    void handleDomainException_givenPaymentException_returns402() {
        PaymentException ex = new PaymentException("Card declined");

        ResponseEntity<ProblemDetail> response = handler.handleDomainException(ex);

        assertEquals(HttpStatus.PAYMENT_REQUIRED, response.getStatusCode());
        ProblemDetail body = response.getBody();
        assertNotNull(body);
        assertEquals(402, body.getStatus());
        assertEquals("Payment Required", body.getTitle());
    }

    @Test
    @DisplayName("handleDomainException - given UnauthorizedException - returns 401 ProblemDetail")
    void handleDomainException_givenUnauthorizedException_returns401() {
        UnauthorizedException ex = new UnauthorizedException("Invalid credentials");

        ResponseEntity<ProblemDetail> response = handler.handleDomainException(ex);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        ProblemDetail body = response.getBody();
        assertNotNull(body);
        assertEquals(401, body.getStatus());
        assertEquals("Unauthorized", body.getTitle());
    }

    @Test
    @DisplayName("handleGeneral - returns 500 ProblemDetail with generic message")
    void handleGeneral_returns500ProblemDetail() {
        WebRequest request = mock(WebRequest.class);
        when(request.getDescription(false)).thenReturn("uri=/api/v1/orders/MH-20260328-0001/calendar");
        RuntimeException ex = new RuntimeException("Something broke");

        ResponseEntity<ProblemDetail> response = handler.handleGeneral(ex, request);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        ProblemDetail body = response.getBody();
        assertNotNull(body);
        assertEquals(500, body.getStatus());
        assertEquals("Internal Server Error", body.getTitle());
        assertEquals("An unexpected error occurred", body.getDetail());
    }

    @Test
    @DisplayName("handleDomainException - returns ProblemDetail with detail field set")
    void handleDomainException_returnsProblemDetailWithDetailField() {
        ResourceNotFoundException ex = new ResourceNotFoundException("Event", "slug", "rock-fest");

        ResponseEntity<ProblemDetail> response = handler.handleDomainException(ex);

        ProblemDetail body = response.getBody();
        assertNotNull(body);
        assertEquals("Event not found with slug: 'rock-fest'", body.getDetail());
        assertEquals("Not Found", body.getTitle());
        assertEquals(404, body.getStatus());
    }

    @Test
    @DisplayName("handleOptimisticLock - returns 409 ProblemDetail with retry message")
    void handleOptimisticLock_returns409ProblemDetail() {
        OptimisticLockingFailureException ex =
                new OptimisticLockingFailureException("Row was updated by another transaction");

        ResponseEntity<ProblemDetail> response = handler.handleOptimisticLock(ex);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        ProblemDetail body = response.getBody();
        assertNotNull(body);
        assertEquals(409, body.getStatus());
        assertEquals("Conflict", body.getTitle());
        assertEquals("This item was modified by another transaction. Please try again.",
                body.getDetail());
    }
}
