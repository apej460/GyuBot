package com.gyubot.user.web;

import com.gyubot.user.exception.DuplicateEmailException;
import com.gyubot.user.exception.DuplicatePendingSignupException;
import com.gyubot.user.exception.InvalidAttachmentException;
import com.gyubot.user.exception.InvalidSignupStatusException;
import com.gyubot.user.exception.MemberNotFoundException;
import com.gyubot.user.exception.PasswordChangeException;
import com.gyubot.user.exception.SignupRequestNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(MemberNotFoundException.class)
    ResponseEntity<Map<String, String>> notFound(MemberNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("code", "MEMBER_NOT_FOUND", "message", e.getMessage()));
    }

    @ExceptionHandler(PasswordChangeException.class)
    ResponseEntity<Map<String, String>> passwordChangeFailed(PasswordChangeException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("code", "PASSWORD_CHANGE_FAILED", "message", e.getMessage()));
    }

    @ExceptionHandler(SignupRequestNotFoundException.class)
    ResponseEntity<Map<String, String>> signupNotFound(SignupRequestNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("code", "SIGNUP_REQUEST_NOT_FOUND", "message", e.getMessage()));
    }

    @ExceptionHandler(InvalidAttachmentException.class)
    ResponseEntity<Map<String, String>> invalidAttachment(InvalidAttachmentException e) {
        return ResponseEntity.badRequest().body(Map.of("code", "INVALID_ATTACHMENT", "message", e.getMessage()));
    }

    @ExceptionHandler(DuplicateEmailException.class)
    ResponseEntity<Map<String, String>> duplicateEmail(DuplicateEmailException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("code", "DUPLICATE_EMAIL", "message", e.getMessage()));
    }

    @ExceptionHandler(DuplicatePendingSignupException.class)
    ResponseEntity<Map<String, String>> duplicatePendingSignup(DuplicatePendingSignupException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("code", "DUPLICATE_PENDING_SIGNUP", "message", e.getMessage()));
    }

    @ExceptionHandler(InvalidSignupStatusException.class)
    ResponseEntity<Map<String, String>> invalidSignupStatus(InvalidSignupStatusException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("code", "INVALID_SIGNUP_STATUS", "message", e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<Map<String, String>> validation(MethodArgumentNotValidException e) {
        return ResponseEntity.badRequest().body(Map.of("code", "VALIDATION_ERROR", "message", "입력값을 확인하세요."));
    }
}
