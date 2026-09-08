package com.nexus.backend.exception;

import com.nexus.backend.dto.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@org.springframework.web.bind.annotation.RestControllerAdvice
@lombok.extern.slf4j.Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ApiResponse<Object>> handleBadRequest(BadRequestException ex) {
        log.warn("Bad request: {}", ex.getMessage());
        return new ResponseEntity<>(
                ApiResponse.error(ex.getMessage()),
                HttpStatus.BAD_REQUEST
        );
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleNotFound(NoResourceFoundException ex) {
        return new ResponseEntity<>(
                ApiResponse.error("Resource not found: " + ex.getResourcePath()),
                HttpStatus.NOT_FOUND
        );
    }

    @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Object>> handleNotReadable(org.springframework.http.converter.HttpMessageNotReadableException ex) {
        log.warn("Payload not readable or parse error: {}", ex.getMessage());
        return new ResponseEntity<>(
                ApiResponse.error("Invalid request payload format: " + ex.getMessage()),
                HttpStatus.OK
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleGlobalException(Exception ex) {
        log.error("Unhandled server exception: ", ex);
        return new ResponseEntity<>(
                ApiResponse.error("Server error: " + ex.getMessage()),
                HttpStatus.OK
        );
    }
}