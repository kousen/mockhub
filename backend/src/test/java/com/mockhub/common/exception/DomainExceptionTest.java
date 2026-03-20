package com.mockhub.common.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DomainExceptionTest {

    @Test
    @DisplayName("DomainException - is sealed with exactly four permitted subclasses")
    void domainException_isSealedWithExactlyFourPermittedSubclasses() {
        assertTrue(DomainException.class.isSealed(),
                "DomainException should be a sealed class");

        Class<?>[] permitted = DomainException.class.getPermittedSubclasses();
        assertEquals(4, permitted.length,
                "DomainException should permit exactly 4 subclasses");
    }

    @Test
    @DisplayName("ResourceNotFoundException - is a DomainException")
    void resourceNotFoundException_isDomainException() {
        ResourceNotFoundException ex = new ResourceNotFoundException("not found");
        assertInstanceOf(DomainException.class, ex,
                "ResourceNotFoundException should extend DomainException");
    }

    @Test
    @DisplayName("ConflictException - is a DomainException")
    void conflictException_isDomainException() {
        ConflictException ex = new ConflictException("conflict");
        assertInstanceOf(DomainException.class, ex,
                "ConflictException should extend DomainException");
    }

    @Test
    @DisplayName("PaymentException - is a DomainException")
    void paymentException_isDomainException() {
        PaymentException ex = new PaymentException("payment failed");
        assertInstanceOf(DomainException.class, ex,
                "PaymentException should extend DomainException");
    }

    @Test
    @DisplayName("UnauthorizedException - is a DomainException")
    void unauthorizedException_isDomainException() {
        UnauthorizedException ex = new UnauthorizedException("unauthorized");
        assertInstanceOf(DomainException.class, ex,
                "UnauthorizedException should extend DomainException");
    }

    @Test
    @DisplayName("PaymentException - preserves cause when constructed with cause")
    void paymentException_preservesCauseWhenConstructedWithCause() {
        RuntimeException cause = new RuntimeException("root cause");
        PaymentException ex = new PaymentException("payment failed", cause);

        assertEquals("payment failed", ex.getMessage());
        assertEquals(cause, ex.getCause());
    }

    @Test
    @DisplayName("ResourceNotFoundException - formats message from resource name, field, and value")
    void resourceNotFoundException_formatsMessageFromResourceNameFieldAndValue() {
        ResourceNotFoundException ex = new ResourceNotFoundException("Event", "slug", "rock-fest");

        assertEquals("Event not found with slug: 'rock-fest'", ex.getMessage());
    }

    @Test
    @DisplayName("Pattern matching switch - handles all DomainException subtypes exhaustively")
    void patternMatchingSwitch_handlesAllDomainExceptionSubtypesExhaustively() {
        // This test verifies that a switch expression over DomainException
        // can exhaustively match all permitted subtypes without a default case.
        // If a 5th subclass is added, this method won't compile until updated.
        DomainException[] exceptions = {
                new ResourceNotFoundException("not found"),
                new ConflictException("conflict"),
                new PaymentException("payment"),
                new UnauthorizedException("unauthorized")
        };

        for (DomainException ex : exceptions) {
            int statusCode = switch (ex) {
                case ResourceNotFoundException _ -> 404;
                case ConflictException _ -> 409;
                case PaymentException _ -> 402;
                case UnauthorizedException _ -> 401;
            };
            assertTrue(statusCode > 0,
                    "Each DomainException subtype should map to a valid HTTP status code");
        }
    }
}
