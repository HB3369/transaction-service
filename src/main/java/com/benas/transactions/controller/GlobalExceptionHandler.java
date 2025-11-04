package com.benas.transactions.controller;

import com.benas.transactions.service.TransactionNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {
    
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    
    @ExceptionHandler(WebExchangeBindException.class)
    public Mono<ResponseEntity<Map<String, Object>>> handleValidationErrors(WebExchangeBindException ex) {
        log.warn("Validation error: {}", ex.getMessage());
        
        var errors = ex.getFieldErrors().stream()
            .collect(Collectors.toMap(
                error -> error.getField(),
                error -> error.getDefaultMessage() != null ? error.getDefaultMessage() : "Invalid value"
            ));
        
        Map<String, Object> response = Map.of(
            "status", "error",
            "code", "VALIDATION_ERROR",
            "message", "Invalid request parameters",
            "errors", errors,
            "timestamp", LocalDateTime.now()
        );
        
        return Mono.just(ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(response));
    }
    
    @ExceptionHandler(TransactionNotFoundException.class)
    public Mono<ResponseEntity<Map<String, Object>>> handleNotFound(TransactionNotFoundException ex) {
        log.warn("Transaction not found: {}", ex.getMessage());
        
        Map<String, Object> response = Map.of(
            "status", "error",
            "code", "NOT_FOUND",
            "message", ex.getMessage(),
            "timestamp", LocalDateTime.now()
        );
        
        return Mono.just(ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(response));
    }
    
    @ExceptionHandler(Exception.class)
    public Mono<ResponseEntity<Map<String, Object>>> handleGenericError(Exception ex) {
        log.error("Unexpected error", ex);
        
        Map<String, Object> response = Map.of(
            "status", "error",
            "code", "INTERNAL_ERROR",
            "message", "An unexpected error occurred",
            "timestamp", LocalDateTime.now()
        );
        
        return Mono.just(ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(response));
    }
}
