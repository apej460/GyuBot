package com.gyubot.search.search.web;

import com.gyubot.search.exception.InternalAuthException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(InternalAuthException.class)
    ResponseEntity<Map<String, String>> internalAuth(InternalAuthException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("code", "INTERNAL_AUTH_FAILED", "message", e.getMessage()));
    }
}
