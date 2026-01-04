package com.threedfly.orderservice.exception;

import jakarta.validation.ConstraintViolation;
import lombok.Getter;

import java.util.Set;

/**
 * Exception thrown when request validation fails.
 * Carries constraint violations for proper error formatting.
 */
@Getter
public class ValidationException extends RuntimeException {

    private final Set<? extends ConstraintViolation<?>> violations;

    public ValidationException(String message, Set<? extends ConstraintViolation<?>> violations) {
        super(message);
        this.violations = violations;
    }
}
