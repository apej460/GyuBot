package com.gyubot.user.web.dto;

import com.gyubot.user.domain.MemberStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateStatusRequest(@NotNull MemberStatus status) {
}
