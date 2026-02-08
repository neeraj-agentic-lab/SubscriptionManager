package com.subscriptionengine.api.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Global exception handler for all API controllers.
 * Provides consistent error responses with meaningful messages.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    
    /**
     * Handle validation errors from @Valid annotations
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationErrors(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new HashMap<>();
        
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(error.getField(), error.getDefaultMessage());
        }
        
        ErrorResponse errorResponse = new ErrorResponse(
            "VALIDATION_ERROR",
            "Validation failed for one or more fields",
            fieldErrors
        );
        
        logger.warn("Validation error: {}", fieldErrors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }
    
    /**
     * Handle database constraint violations
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        String message = ex.getMessage() != null ? ex.getMessage().toLowerCase() : "";
        String errorCode = "DATA_INTEGRITY_ERROR";
        String userMessage = "Data integrity constraint violation";
        HttpStatus status = HttpStatus.BAD_REQUEST;
        
        // Parse specific constraint violations
        if (message.contains("constraint")) {
            if (message.contains("subscriptions_status_check")) {
                errorCode = "INVALID_STATUS";
                userMessage = "Invalid subscription status. Allowed values: ACTIVE, TRIALING, PAUSED, CANCELED, EXPIRED, PAST_DUE";
            } else if (message.contains("subscriptions_trial_check")) {
                errorCode = "INVALID_TRIAL_PERIOD";
                userMessage = "Trial end date must be after trial start date";
            } else if (message.contains("plans_interval_check")) {
                errorCode = "INVALID_BILLING_INTERVAL";
                userMessage = "Invalid billing interval. Allowed values: DAILY, WEEKLY, MONTHLY, QUARTERLY, YEARLY";
            } else if (message.contains("plans_type_check")) {
                errorCode = "INVALID_PLAN_TYPE";
                userMessage = "Invalid plan type. Allowed values: RECURRING, ONE_TIME";
            } else if (message.contains("duplicate key") || message.contains("unique constraint")) {
                errorCode = "DUPLICATE_ENTRY";
                status = HttpStatus.CONFLICT; // 409 for duplicates
                if (message.contains("tenants_slug_key")) {
                    userMessage = "A tenant with this slug already exists";
                } else {
                    userMessage = "A record with this value already exists";
                }
            } else if (message.contains("foreign key constraint")) {
                errorCode = "INVALID_REFERENCE";
                if (message.contains("plans_tenant_id_fkey")) {
                    userMessage = "The specified tenant does not exist";
                } else if (message.contains("subscriptions_plan_id_fkey")) {
                    userMessage = "The specified plan does not exist";
                } else if (message.contains("subscriptions_customer_id_fkey")) {
                    userMessage = "The specified customer does not exist";
                } else {
                    userMessage = "Referenced record does not exist";
                }
            }
        }
        
        ErrorResponse errorResponse = new ErrorResponse(
            errorCode,
            userMessage,
            Map.of("details", extractConstraintDetails(message))
        );
        
        logger.error("Data integrity violation: {}", errorCode, ex);
        return ResponseEntity.status(status).body(errorResponse);
    }
    
    /**
     * Handle type mismatch exceptions (e.g., invalid UUID format)
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String paramName = ex.getName();
        String requiredType = ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown";
        
        ErrorResponse errorResponse = new ErrorResponse(
            "INVALID_PARAMETER_FORMAT",
            String.format("Invalid format for parameter '%s'. Expected type: %s", paramName, requiredType),
            Map.of("parameter", paramName, "providedValue", ex.getValue() != null ? ex.getValue().toString() : "null")
        );
        
        logger.warn("Type mismatch for parameter '{}': {}", paramName, ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }
    
    /**
     * Handle generic exceptions
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        ErrorResponse errorResponse = new ErrorResponse(
            "INTERNAL_ERROR",
            "An unexpected error occurred. Please contact support if this persists.",
            Map.of("type", ex.getClass().getSimpleName())
        );
        
        logger.error("Unexpected error", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
    
    /**
     * Extract constraint details from error message
     */
    private String extractConstraintDetails(String message) {
        if (message == null) {
            return "No additional details available";
        }
        
        // Extract detail line if present
        int detailIndex = message.indexOf("Detail:");
        if (detailIndex != -1) {
            int endIndex = message.indexOf("\n", detailIndex);
            if (endIndex == -1) {
                endIndex = message.length();
            }
            return message.substring(detailIndex, endIndex).trim();
        }
        
        return "Check your input values and try again";
    }
    
    /**
     * Standard error response structure
     */
    public static class ErrorResponse {
        private final boolean success = false;
        private final String error;
        private final String message;
        private final Map<String, String> details;
        private final OffsetDateTime timestamp;
        
        public ErrorResponse(String error, String message, Map<String, String> details) {
            this.error = error;
            this.message = message;
            this.details = details;
            this.timestamp = OffsetDateTime.now();
        }
        
        public boolean isSuccess() {
            return success;
        }
        
        public String getError() {
            return error;
        }
        
        public String getMessage() {
            return message;
        }
        
        public Map<String, String> getDetails() {
            return details;
        }
        
        public OffsetDateTime getTimestamp() {
            return timestamp;
        }
    }
}
