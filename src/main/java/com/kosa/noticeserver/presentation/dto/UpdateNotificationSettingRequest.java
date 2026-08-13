package com.kosa.noticeserver.presentation.dto;

import com.kosa.noticeserver.domain.model.NotificationType;
import jakarta.validation.constraints.NotNull;

public record UpdateNotificationSettingRequest(
        @NotNull NotificationType type,
        @NotNull Boolean enabled
) {
}
