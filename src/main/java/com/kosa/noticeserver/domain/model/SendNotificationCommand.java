package com.kosa.noticeserver.domain.model;

import java.util.Map;

public record SendNotificationCommand(
        String target,
        String title,
        String body,
        Map<String, String> data,
        ChannelType type
) {
}
