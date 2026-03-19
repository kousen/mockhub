package com.mockhub.common.exception;

public abstract sealed class DomainException extends RuntimeException
        permits ResourceNotFoundException, ConflictException,
                PaymentException, UnauthorizedException {

    protected DomainException(String message) {
        super(message);
    }

    protected DomainException(String message, Throwable cause) {
        super(message, cause);
    }
}
