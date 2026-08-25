package com.gyubot.user.exception;

public class InvalidAttachmentException extends RuntimeException {
    public InvalidAttachmentException() {
        super("첨부파일은 JPG, PNG, PDF 형식만 가능합니다.");
    }
}
