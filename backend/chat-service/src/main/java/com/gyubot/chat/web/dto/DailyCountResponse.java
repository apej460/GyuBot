package com.gyubot.chat.web.dto;

import java.time.LocalDate;

public record DailyCountResponse(LocalDate date, int count) {
}
