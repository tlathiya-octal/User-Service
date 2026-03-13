package com.ecommerce.user.exception;

import com.ecommerce.user.filter.CorrelationIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(UserNotFoundException exception, HttpServletRequest request) {
        return buildError(HttpStatus.NOT_FOUND, exception.getMessage(), request, List.of());
    }

    @ExceptionHandler(DuplicateUserException.class)
    public ResponseEntity<ApiError> handleConflict(DuplicateUserException exception, HttpServletRequest request) {
        return buildError(HttpStatus.CONFLICT, exception.getMessage(), request, List.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException exception,
                                                     HttpServletRequest request) {
        List<String> validationErrors = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(this::formatFieldError)
                .toList();
        return buildError(HttpStatus.BAD_REQUEST, "Validation failed", request, validationErrors);
    }

    @ExceptionHandler(JwtException.class)
    public ResponseEntity<ApiError> handleJwt(JwtException exception, HttpServletRequest request) {
        return buildError(HttpStatus.UNAUTHORIZED, "Invalid JWT token", request, List.of());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGeneric(Exception exception, HttpServletRequest request) {
        log.error("Unhandled exception", exception);
        return buildError(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error", request, List.of());
    }

    private ResponseEntity<ApiError> buildError(HttpStatus status,
                                                String message,
                                                HttpServletRequest request,
                                                List<String> validationErrors) {
        Object correlationId = request.getHeader(CorrelationIdFilter.HEADER_NAME) != null
                ? request.getHeader(CorrelationIdFilter.HEADER_NAME)
                : request.getAttribute(CorrelationIdFilter.REQUEST_ATTRIBUTE);
        ApiError apiError = new ApiError(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                request.getRequestURI(),
                correlationId != null ? correlationId.toString() : null,
                validationErrors
        );
        return ResponseEntity.status(status).body(apiError);
    }

    private String formatFieldError(FieldError fieldError) {
        return fieldError.getField() + ": " + fieldError.getDefaultMessage();
    }
}
