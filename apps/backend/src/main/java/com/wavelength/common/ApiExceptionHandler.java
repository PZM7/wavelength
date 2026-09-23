package com.wavelength.common;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.net.URISyntaxException;
import java.time.DateTimeException;

@RestControllerAdvice
public class ApiExceptionHandler {
    private static final Logger LOG = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiError> domain(ApiException e, HttpServletRequest req) {
        return response(e.getStatus(), e.getCode(), e.getMessage(), req);
    }

    @ExceptionHandler({
        MethodArgumentNotValidException.class,
        ConstraintViolationException.class,
        HttpMessageNotReadableException.class,
        MethodArgumentTypeMismatchException.class,
        IllegalArgumentException.class,
        DateTimeException.class,
        URISyntaxException.class
    })
    public ResponseEntity<ApiError> invalid(Exception e, HttpServletRequest req) {
        return response(
                HttpStatus.BAD_REQUEST,
                "VALIDATION_ERROR",
                "Invalid request fields or parameters",
                req);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> constraint(Exception e, HttpServletRequest req) {
        return response(
                HttpStatus.CONFLICT,
                "RESOURCE_CONFLICT",
                "Resource conflicts with existing data",
                req);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiError> missing(Exception e, HttpServletRequest req) {
        return response(HttpStatus.NOT_FOUND, "NOT_FOUND", "Resource not found", req);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> unexpected(Exception e, HttpServletRequest req) {
        // Exception messages and stack traces may contain sensitive values.
        LOG.error("Unhandled request failure: {}", e.getClass().getSimpleName());
        return response(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "INTERNAL_ERROR",
                "An unexpected error occurred",
                req);
    }

    private ResponseEntity<ApiError> response(
            HttpStatus status, String code, String message, HttpServletRequest req) {
        return ResponseEntity.status(status).body(new ApiError(code, message, req.getRequestURI()));
    }
}
