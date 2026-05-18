package com.kosa.noticeserver.infrastructure.sender.fcm;

import com.google.firebase.messaging.*;
import com.kosa.noticeserver.domain.model.*;
import com.kosa.noticeserver.infrastructure.sender.NotificationSender;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
public class FCMSender implements NotificationSender {

    private final FirebaseMessaging firebaseMessaging;

    @Autowired
    public FCMSender(FirebaseMessaging firebaseMessaging) {
        this.firebaseMessaging = firebaseMessaging;
    }

    @Override
    public void send(SendNotificationCommand command) throws Throwable {
        firebaseMessaging.send(buildMessage(command), true);
    }

    public SendBatchResult send(List<SendNotificationCommand> commands) throws Throwable {
        BatchResponse batchResponse = firebaseMessaging.sendEach(
                commands.stream().map(this::buildMessage).toList(),
                true
        );

        List<SendDetails> results = new ArrayList<>();
        for (int i = 0; i < batchResponse.getResponses().size(); i++) {
            SendResponse sendResponse = batchResponse.getResponses().get(i);
            SendNotificationCommand originalCommand = commands.get(i);

            results.add(new SendDetails(
                    sendResponse.isSuccessful(),
                    sendResponse.getMessageId(),
                    !sendResponse.isSuccessful() ? sendResponse.getException().getErrorCode().toString() : null,
                    originalCommand
            ));
        }

        return new SendBatchResult(results, batchResponse.getSuccessCount(), batchResponse.getFailureCount());
    }

    @Override
    public boolean supports(ChannelType type) {
        return false;
    }

    private Message buildMessage(SendNotificationCommand command) {
        return Message.builder()
                .setToken(command.target())
                .setNotification(
                        Notification.builder()
                            .setTitle(command.title())
                            .setBody(command.body())
                            .build())
                .build();
    }
}
