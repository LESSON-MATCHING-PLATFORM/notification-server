package com.kosa.noticeserver.presentaion.dto;

import com.kosa.noticeserver.domain.model.NotificationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UpdateNotificationSettingRequest(
        @NotBlank String userId,
        @NotNull NotificationType type,
        @NotNull Boolean enabled
) {
}
