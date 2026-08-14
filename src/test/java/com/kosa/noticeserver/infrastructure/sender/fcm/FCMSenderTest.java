package com.kosa.noticeserver.infrastructure.sender.fcm;

import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.SendResponse;
import com.kosa.noticeserver.config.FirebaseProperties;
import com.kosa.noticeserver.domain.model.SendNotificationCommand;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FCMSenderTest {

    @Test
    void 단건_메시지_발송시_실제_발송_설정을_전달한다() throws Throwable {
        FirebaseMessaging firebaseMessaging = mock(FirebaseMessaging.class);
        FirebaseProperties properties = properties(false);
        FCMSender fcmSender = new FCMSender(firebaseMessaging, properties);
        SendNotificationCommand sendNotificationCommand = getSendNotificationCommand("Test", "Spring Message Test");

        when(firebaseMessaging.send(any(), eq(false))).thenReturn("message-001");

        fcmSender.send(sendNotificationCommand);

        verify(firebaseMessaging).send(any(), eq(false));
    }

    @Test
    void 단건_메시지_발송시_dry_run_설정을_전달한다() throws Throwable {
        FirebaseMessaging firebaseMessaging = mock(FirebaseMessaging.class);
        FirebaseProperties properties = properties(true);
        FCMSender fcmSender = new FCMSender(firebaseMessaging, properties);
        SendNotificationCommand sendNotificationCommand = getSendNotificationCommand("Test", "Spring Message Test");

        when(firebaseMessaging.send(any(), eq(true))).thenReturn("message-001");

        fcmSender.send(sendNotificationCommand);

        verify(firebaseMessaging).send(any(), eq(true));
    }

    @Test
    void 여러개의_메시지_발송시_dry_run_설정을_전달한다() throws Throwable {
        FirebaseMessaging firebaseMessaging = mock(FirebaseMessaging.class);
        FirebaseProperties properties = properties(true);
        FCMSender fcmSender = new FCMSender(firebaseMessaging, properties);
        List<SendNotificationCommand> list = List.of(
                getSendNotificationCommand("Test1", "Spring Message Test1"),
                getSendNotificationCommand("Test2", "Spring Message Test2"),
                getSendNotificationCommand("Test3", "Spring Message Test3"),
                getSendNotificationCommand("Test4", "Spring Message Test4"),
                getSendNotificationCommand("Test5", "Spring Message Test5")
        );

        BatchResponse batchResponse = mock(BatchResponse.class);
        SendResponse sendResponse = mock(SendResponse.class);
        when(firebaseMessaging.sendEach(anyList(), eq(true))).thenReturn(batchResponse);
        when(batchResponse.getResponses()).thenReturn(List.of(sendResponse));
        when(sendResponse.isSuccessful()).thenReturn(true);
        when(batchResponse.getSuccessCount()).thenReturn(1);
        when(batchResponse.getFailureCount()).thenReturn(0);

        fcmSender.send(list);

        verify(firebaseMessaging).sendEach(anyList(), eq(true));
    }

    private static FirebaseProperties properties(boolean dryRun) {
        FirebaseProperties properties = new FirebaseProperties();
        properties.setDryRun(dryRun);
        return properties;
    }

    private static SendNotificationCommand getSendNotificationCommand(String title, String body) {
        return new SendNotificationCommand(
                "d6UgfPSy4wc221Wsv3wQnQ:APA91bHlmQgskhLyw7MovsRWYJovMa902JXhtzlyBPUE8y1VqMgYI6Q4Mdd8ducJv9qEdZp8ProeF9TVXOKD6xhpS2zpx9q3F9N_pbLYrWJa6PNpjQHoCSA",
                title,
                body,
                Collections.emptyMap(),
                null
        );
    }
}
