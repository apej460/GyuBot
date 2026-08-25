package com.gyubot.auth.web.dto;

public record LoginResponse(boolean otpRequired, String message) {
}
