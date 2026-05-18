package com.kosa.noticeserver.domain.model;

import java.util.List;
import java.util.Map;

public record SendDetails(
        boolean isSuccess,
        String messageId,
        String errorCode,
        SendNotificationCommand originalCommand
) {
}
