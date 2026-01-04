package com.threedfly.orderservice.config;

import com.threedfly.orderservice.exception.FileParseException;
import com.threedfly.orderservice.exception.InvalidFileTypeException;
import com.threedfly.orderservice.exception.InvalidParameterCombinationException;
import com.threedfly.orderservice.exception.ValidationException;
import jakarta.validation.ConstraintViolation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntimeException(RuntimeException ex) {
        log.error("Runtime exception occurred: {}", ex.getMessage(), ex);
        
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("timestamp", LocalDateTime.now());
        errorResponse.put("status", HttpStatus.NOT_FOUND.value());
        errorResponse.put("error", "Not Found");
        errorResponse.put("message", ex.getMessage());
        
        // Check if it's a "not found" type error
        if (ex.getMessage() != null && ex.getMessage().toLowerCase().contains("not found")) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
        }
        
        // Default to internal server error for other runtime exceptions
        errorResponse.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
        errorResponse.put("error", "Internal Server Error");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationException(MethodArgumentNotValidException ex) {
        log.error("Validation exception occurred: {}", ex.getMessage());

        // Check if this is a material combination validation error
        boolean isMaterialCombinationError = ex.getBindingResult().getAllErrors().stream()
                .anyMatch(error -> error.getCode() != null &&
                         error.getCode().equals("ValidMaterialCombination"));

        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("timestamp", LocalDateTime.now());
        errorResponse.put("status", HttpStatus.BAD_REQUEST.value());

        if (isMaterialCombinationError) {
            // Return specific error format for material combination errors
            errorResponse.put("error", "Invalid Parameter Combination");
            String message = ex.getBindingResult().getAllErrors().get(0).getDefaultMessage();
            errorResponse.put("message", message);
        } else {
            // Return field errors for other validation failures
            errorResponse.put("error", "Validation Failed");
            Map<String, String> fieldErrors = new HashMap<>();
            for (FieldError error : ex.getBindingResult().getFieldErrors()) {
                fieldErrors.put(error.getField(), error.getDefaultMessage());
            }
            errorResponse.put("fieldErrors", fieldErrors);
        }

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<Map<String, Object>> handleValidationException(ValidationException ex) {
        log.error("Manual validation exception occurred: {}", ex.getMessage());

        // Check if this is a material combination validation error
        boolean isMaterialCombinationError = ex.getViolations().stream()
                .anyMatch(violation -> {
                    // Check if the constraint annotation is ValidMaterialCombination
                    return violation.getConstraintDescriptor().getAnnotation().annotationType().getSimpleName()
                            .equals("ValidMaterialCombination");
                });

        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("timestamp", LocalDateTime.now());
        errorResponse.put("status", HttpStatus.BAD_REQUEST.value());

        if (isMaterialCombinationError) {
            // Return specific error format for material combination errors
            errorResponse.put("error", "Invalid Parameter Combination");
            String message = ex.getViolations().iterator().next().getMessage();
            errorResponse.put("message", message);
        } else {
            // Return field errors for other validation failures
            errorResponse.put("error", "Validation Failed");
            Map<String, String> fieldErrors = new HashMap<>();
            for (ConstraintViolation<?> violation : ex.getViolations()) {
                fieldErrors.put(violation.getPropertyPath().toString(), violation.getMessage());
            }
            errorResponse.put("fieldErrors", fieldErrors);
        }

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgumentException(IllegalArgumentException ex) {
        log.error("Illegal argument exception occurred: {}", ex.getMessage(), ex);
        
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("timestamp", LocalDateTime.now());
        errorResponse.put("status", HttpStatus.BAD_REQUEST.value());
        errorResponse.put("error", "Bad Request");
        errorResponse.put("message", ex.getMessage());
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(org.springframework.web.HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Map<String, Object>> handleMethodNotSupported(org.springframework.web.HttpRequestMethodNotSupportedException ex) {
        log.error("Method not supported: {}", ex.getMessage());
        
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("timestamp", LocalDateTime.now());
        errorResponse.put("status", HttpStatus.METHOD_NOT_ALLOWED.value());
        errorResponse.put("error", "Method Not Allowed");
        errorResponse.put("message", ex.getMessage());
        
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(errorResponse);
    }

    @ExceptionHandler(org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> handleTypeMismatch(org.springframework.web.method.annotation.MethodArgumentTypeMismatchException ex) {
        log.error("Type conversion error: {}", ex.getMessage());
        
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("timestamp", LocalDateTime.now());
        errorResponse.put("status", HttpStatus.BAD_REQUEST.value());
        errorResponse.put("error", "Bad Request");
        errorResponse.put("message", "Invalid parameter value: " + ex.getValue());
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(org.springframework.web.multipart.MultipartException.class)
    public ResponseEntity<Map<String, Object>> handleMultipartException(org.springframework.web.multipart.MultipartException ex) {
        log.error("Multipart exception: {}", ex.getMessage());

        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("timestamp", LocalDateTime.now());
        errorResponse.put("status", HttpStatus.BAD_REQUEST.value());
        errorResponse.put("error", "Bad Request");
        errorResponse.put("message", "Multipart request required");

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(org.springframework.web.multipart.support.MissingServletRequestPartException.class)
    public ResponseEntity<Map<String, Object>> handleMissingServletRequestPartException(
            org.springframework.web.multipart.support.MissingServletRequestPartException ex) {
        log.error("Missing request part: {}", ex.getMessage());

        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("timestamp", LocalDateTime.now());
        errorResponse.put("status", HttpStatus.BAD_REQUEST.value());
        errorResponse.put("error", "Bad Request");
        errorResponse.put("message", "File is required. Please upload a 3D model file (STL, OBJ, or 3MF)");
        errorResponse.put("missingPart", ex.getRequestPartName());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(InvalidFileTypeException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidFileType(InvalidFileTypeException ex) {
        log.error("Invalid file type: {}", ex.getMessage());

        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("timestamp", LocalDateTime.now());
        errorResponse.put("status", HttpStatus.BAD_REQUEST.value());
        errorResponse.put("error", "Invalid File Type");
        errorResponse.put("message", ex.getMessage());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(InvalidParameterCombinationException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidParameterCombination(
            InvalidParameterCombinationException ex) {
        log.error("Invalid parameter combination: {}", ex.getMessage());

        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("timestamp", LocalDateTime.now());
        errorResponse.put("status", HttpStatus.BAD_REQUEST.value());
        errorResponse.put("error", "Invalid Parameter Combination");
        errorResponse.put("message", ex.getMessage());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(FileParseException.class)
    public ResponseEntity<Map<String, Object>> handleFileParseException(FileParseException ex) {
        log.error("File parse error: {}", ex.getMessage(), ex);

        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("timestamp", LocalDateTime.now());
        errorResponse.put("status", HttpStatus.UNPROCESSABLE_ENTITY.value());
        errorResponse.put("error", "File Parse Error");
        errorResponse.put("message", ex.getMessage());

        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(errorResponse);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {
        log.error("Unexpected exception occurred: {}", ex.getMessage(), ex);

        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("timestamp", LocalDateTime.now());
        errorResponse.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
        errorResponse.put("error", "Internal Server Error");
        errorResponse.put("message", "An unexpected error occurred");

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
} 