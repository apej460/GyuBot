package com.gyubot.auth.web;

import com.gyubot.auth.exception.InternalAuthException;
import com.gyubot.auth.exception.InvalidCredentialsException;
import com.gyubot.auth.exception.OtpVerificationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(InvalidCredentialsException.class)
    ResponseEntity<Map<String, String>> invalidCredentials(InvalidCredentialsException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("code", "INVALID_CREDENTIALS", "message", e.getMessage()));
    }

    @ExceptionHandler(OtpVerificationException.class)
    ResponseEntity<Map<String, String>> otpFailed(OtpVerificationException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("code", "OTP_VERIFICATION_FAILED", "message", e.getMessage()));
    }

    @ExceptionHandler(InternalAuthException.class)
    ResponseEntity<Map<String, String>> internalAuth(InternalAuthException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("code", "INTERNAL_AUTH_FAILED", "message", e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<Map<String, String>> validation(MethodArgumentNotValidException e) {
        return ResponseEntity.badRequest().body(Map.of("code", "VALIDATION_ERROR", "message", "입력값을 확인하세요."));
    }
}
