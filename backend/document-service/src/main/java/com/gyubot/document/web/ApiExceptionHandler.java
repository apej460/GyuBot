package com.gyubot.document.web;

import com.gyubot.document.exception.DocumentNotFoundException;
import com.gyubot.document.exception.InvalidDocumentFileException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(DocumentNotFoundException.class)
    ResponseEntity<Map<String, String>> notFound(DocumentNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("code", "DOCUMENT_NOT_FOUND", "message", e.getMessage()));
    }

    @ExceptionHandler(InvalidDocumentFileException.class)
    ResponseEntity<Map<String, String>> invalidFile(InvalidDocumentFileException e) {
        return ResponseEntity.badRequest().body(Map.of("code", "INVALID_DOCUMENT_FILE", "message", e.getMessage()));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    ResponseEntity<Map<String, String>> tooLarge(MaxUploadSizeExceededException e) {
        return ResponseEntity.badRequest()
                .body(Map.of("code", "INVALID_DOCUMENT_FILE", "message", "파일 크기는 50MB를 초과할 수 없습니다."));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<Map<String, String>> validation(MethodArgumentNotValidException e) {
        return ResponseEntity.badRequest().body(Map.of("code", "VALIDATION_ERROR", "message", "입력값을 확인하세요."));
    }
}
