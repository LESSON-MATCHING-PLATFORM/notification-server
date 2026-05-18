package com.kosa.noticeserver.infrastructure.sender;

import com.kosa.noticeserver.domain.model.ChannelType;
import com.kosa.noticeserver.domain.model.SendBatchResult;
import com.kosa.noticeserver.domain.model.SendNotificationCommand;

import java.util.List;

public interface NotificationSender {
    boolean supports(ChannelType type);
    void send(SendNotificationCommand command) throws Throwable;
    SendBatchResult send(List<SendNotificationCommand> commands) throws Throwable;
}
