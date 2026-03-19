package com.mockhub.common.exception;

public abstract sealed class DomainException extends RuntimeException
        permits ResourceNotFoundException, ConflictException,
                PaymentException, UnauthorizedException {

    public DomainException(String message) {
        super(message);
    }

    public DomainException(String message, Throwable cause) {
        super(message, cause);
    }
}
