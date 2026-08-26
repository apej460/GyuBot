package com.gyubot.chat.exception;

public class ChatSessionNotFoundException extends RuntimeException {
    public ChatSessionNotFoundException() {
        super("대화방을 찾을 수 없습니다.");
    }
}
