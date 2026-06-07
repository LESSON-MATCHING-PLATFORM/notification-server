package com.kosa.noticeserver.domain.model;

public record SendDetails(
        boolean isSuccess,
        String messageId,
        String errorMessage,
        String errorCode,
        SendNotificationCommand originalCommand
) {
}
