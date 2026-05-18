package com.kosa.noticeserver.infrastructure.sender.fcm;

import com.kosa.noticeserver.domain.model.SendNotificationCommand;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class FCMSenderTest {

    @Autowired
    private FCMSender fcmSender;

    @Test
    @DisplayName("한개의 메시지를 발송합니다.")
    public void send() {
        SendNotificationCommand sendNotificationCommand = getSendNotificationCommand("Test", "Spring Message Test");

        assertDoesNotThrow(() -> fcmSender.send(sendNotificationCommand));
    }

    @Test
    @DisplayName("여러개의 메시지를 발송합니다.")
    public void multisend() {
        List<SendNotificationCommand> list = List.of(
                getSendNotificationCommand("Test1", "Spring Message Test1"),
                getSendNotificationCommand("Test2", "Spring Message Test2"),
                getSendNotificationCommand("Test3", "Spring Message Test3"),
                getSendNotificationCommand("Test4", "Spring Message Test4"),
                getSendNotificationCommand("Test5", "Spring Message Test5")
        );

        assertDoesNotThrow(() -> fcmSender.send(list));
    }

    private static SendNotificationCommand getSendNotificationCommand(String title, String body) {
        return new SendNotificationCommand(
                "d6UgfPSy4wc221Wsv3wQnQ:APA91bHlmQgskhLyw7MovsRWYJovMa902JXhtzlyBPUE8y1VqMgYI6Q4Mdd8ducJv9qEdZp8ProeF9TVXOKD6xhpS2zpx9q3F9N_pbLYrWJa6PNpjQHoCSA",
                title,
                body,
                Collections.emptyMap(),
                null,
                null
        );
    }
}