package com.kosa.noticeserver.domain.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kosa.noticeserver.domain.model.ChannelType;
import com.kosa.noticeserver.domain.model.NotificationDelivery;
import com.kosa.noticeserver.domain.model.NotificationType;
import com.kosa.noticeserver.domain.model.SendBatchResult;
import com.kosa.noticeserver.domain.model.SendDetails;
import com.kosa.noticeserver.domain.model.SendNotificationCommand;
import com.kosa.noticeserver.domain.model.TokenEntity;
import com.kosa.noticeserver.domain.model.event.PaymentEvent;
import com.kosa.noticeserver.infrastructure.repository.TokenRepository;
import com.kosa.noticeserver.infrastructure.sender.fcm.FCMSender;
import com.kosa.noticeserver.infrastructure.sender.fcm.FcmFailureClassifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PaymentEventNotificationServiceTest {

    private final TokenRepository tokenRepository = mock(TokenRepository.class);
    private final FCMSender fcmSender = mock(FCMSender.class);
    private final NotificationService notificationService = mock(NotificationService.class);
    private final NotificationDeliveryService notificationDeliveryService = mock(NotificationDeliveryService.class);
    private final FcmFailureClassifier fcmFailureClassifier = mock(FcmFailureClassifier.class);
    private final PaymentEventNotificationService service = new PaymentEventNotificationService(
            tokenRepository,
            fcmSender,
            notificationService,
            notificationDeliveryService,
            fcmFailureClassifier
    );

    @Test
    @DisplayName("FCM batch 요청 자체가 실패하면 delivery 실패로 기록하고 예외를 전파하지 않는다")
    void notice_whenFcmBatchRequestFails_doesNotPropagateRetryableException() throws Throwable {
        PaymentEvent event = paymentEvent();

        when(notificationService.canSend("user-001", NotificationType.PAYMENT)).thenReturn(true);
        when(tokenRepository.findAllTokensByUserId("user-001")).thenReturn(List.of("token-001"));
        when(notificationDeliveryService.claimPaymentFcmDelivery("event-001", "user-001"))
                .thenReturn(Optional.of(delivery("event-001", "user-001")));
        when(fcmSender.send(anyList())).thenThrow(new IllegalStateException("fcm down"));

        assertDoesNotThrow(() -> service.notice(event, "event-001"));
        verify(notificationDeliveryService).markFailed(any(NotificationDelivery.class), eq("fcm down"));
    }

    @Test
    @DisplayName("같은 eventId와 사용자 조합이 이미 처리 중이면 FCM 발송을 건너뛴다")
    void notice_whenDeliveryAlreadyClaimed_skipsFcmSend() throws Throwable {
        PaymentEvent event = paymentEvent();

        when(notificationService.canSend("user-001", NotificationType.PAYMENT)).thenReturn(true);
        when(tokenRepository.findAllTokensByUserId("user-001")).thenReturn(List.of("token-001"));
        when(notificationDeliveryService.claimPaymentFcmDelivery("event-001", "user-001"))
                .thenReturn(Optional.empty());

        service.notice(event, "event-001");

        verify(fcmSender, never()).send(anyList());
    }

    @Test
    @DisplayName("알림 수신 설정이 꺼진 사용자는 토큰 조회와 FCM 발송을 건너뛴다")
    void notice_whenSendSettingIsDisabled_skipsTokenLookupAndFcmSend() throws Throwable {
        PaymentEvent event = paymentEvent();

        when(notificationService.canSend("user-001", NotificationType.PAYMENT)).thenReturn(false);

        service.notice(event, "event-001");

        verify(notificationService).canSend("user-001", NotificationType.PAYMENT);
        verify(tokenRepository, never()).findAllTokensByUserId("user-001");
        verify(notificationDeliveryService, never()).claimPaymentFcmDelivery(any(), any());
        verify(fcmSender, never()).send(anyList());
    }

    @Test
    @DisplayName("FCM 토큰이 없는 사용자는 delivery claim과 FCM 발송을 건너뛴다")
    void notice_whenTokenIsMissing_skipsDeliveryClaimAndFcmSend() throws Throwable {
        PaymentEvent event = paymentEvent();

        when(notificationService.canSend("user-001", NotificationType.PAYMENT)).thenReturn(true);
        when(tokenRepository.findAllTokensByUserId("user-001")).thenReturn(List.of());

        service.notice(event, "event-001");

        verify(notificationService).canSend("user-001", NotificationType.PAYMENT);
        verify(tokenRepository).findAllTokensByUserId("user-001");
        verify(notificationDeliveryService, never()).claimPaymentFcmDelivery(any(), any());
        verify(fcmSender, never()).send(anyList());
    }

    @Test
    @DisplayName("같은 eventId와 사용자 조합을 두 번 처리해도 FCM은 한 번만 발송한다")
    void notice_whenSameEventAndUserIsConsumedTwice_sendsFcmOnce() throws Throwable {
        PaymentEvent event = paymentEvent();
        NotificationDelivery delivery = delivery("event-001", "user-001");

        when(notificationService.canSend("user-001", NotificationType.PAYMENT)).thenReturn(true);
        when(tokenRepository.findAllTokensByUserId("user-001")).thenReturn(List.of("token-001"));
        when(notificationDeliveryService.claimPaymentFcmDelivery("event-001", "user-001"))
                .thenReturn(Optional.of(delivery))
                .thenReturn(Optional.empty());
        when(fcmSender.send(anyList())).thenReturn(new SendBatchResult(
                List.of(new SendDetails(true, "message-001", null, null, command("token-001", "user-001"))),
                1,
                0
        ));

        service.notice(event, "event-001");
        service.notice(event, "event-001");

        verify(fcmSender, times(1)).send(anyList());
        ArgumentCaptor<List<SendNotificationCommand>> captor = ArgumentCaptor.forClass(List.class);
        verify(fcmSender).send(captor.capture());
        Map<String, String> data = captor.getValue().getFirst().data();
        assertThat(data).containsEntry("eventId", "event-001");
        assertThat(data).containsEntry("notificationType", NotificationType.PAYMENT.name());
        assertThat(data).containsEntry("dedupKey", "PAYMENT_COMPLETED:order-001");
        verify(notificationDeliveryService, times(1)).markSent(delivery);
        verify(notificationDeliveryService, never()).markFailed(any(NotificationDelivery.class), any());
    }

    @Test
    @DisplayName("FCM 응답이 invalid token이면 토큰을 정리하고 delivery 실패로 기록한다")
    void notice_whenFcmResultContainsInvalidToken_deletesToken() throws Throwable {
        PaymentEvent event = paymentEvent();
        NotificationDelivery delivery = delivery("event-001", "user-001");

        when(notificationService.canSend("user-001", NotificationType.PAYMENT)).thenReturn(true);
        when(tokenRepository.findAllTokensByUserId("user-001")).thenReturn(List.of("token-001"));
        when(notificationDeliveryService.claimPaymentFcmDelivery("event-001", "user-001"))
                .thenReturn(Optional.of(delivery));
        when(fcmSender.send(anyList())).thenAnswer(invocation -> {
            List<SendNotificationCommand> commands = invocation.getArgument(0);
            return new SendBatchResult(
                    List.of(new SendDetails(false, null, "token unregistered", "UNREGISTERED", commands.getFirst())),
                    0,
                    1
            );
        });
        when(fcmFailureClassifier.isInvalidToken(any(SendDetails.class))).thenReturn(true);

        service.notice(event, "event-001");

        verify(tokenRepository).deleteByTokenIn(List.of("token-001"));
        verify(notificationDeliveryService).markFailed(delivery, "token unregistered");
    }

    @Test
    @DisplayName("FCM 응답 실패가 invalid token이 아니면 토큰을 삭제하지 않는다")
    void notice_whenFcmResultFailureIsNotInvalidToken_keepsToken() throws Throwable {
        PaymentEvent event = paymentEvent();
        NotificationDelivery delivery = delivery("event-001", "user-001");

        when(notificationService.canSend("user-001", NotificationType.PAYMENT)).thenReturn(true);
        when(tokenRepository.findAllTokensByUserId("user-001")).thenReturn(List.of("token-001"));
        when(notificationDeliveryService.claimPaymentFcmDelivery("event-001", "user-001"))
                .thenReturn(Optional.of(delivery));
        when(fcmSender.send(anyList())).thenAnswer(invocation -> {
            List<SendNotificationCommand> commands = invocation.getArgument(0);
            return new SendBatchResult(
                    List.of(new SendDetails(false, null, "fcm unavailable", "UNAVAILABLE", commands.getFirst())),
                    0,
                    1
            );
        });
        when(fcmFailureClassifier.isInvalidToken(any(SendDetails.class))).thenReturn(false);

        service.notice(event, "event-001");

        verify(tokenRepository, never()).deleteByTokenIn(anyList());
        verify(notificationDeliveryService).markFailed(delivery, "fcm unavailable");
    }

    @Test
    @DisplayName("invalid token 정리에 실패해도 delivery 성공 기록은 유지한다")
    void notice_whenInvalidTokenCleanupFails_marksDeliveryResult() throws Throwable {
        PaymentEvent event = paymentEvent();
        NotificationDelivery delivery = delivery("event-001", "user-001");

        when(notificationService.canSend("user-001", NotificationType.PAYMENT)).thenReturn(true);
        when(tokenRepository.findAllTokensByUserId("user-001")).thenReturn(List.of("token-001", "token-002"));
        when(notificationDeliveryService.claimPaymentFcmDelivery("event-001", "user-001"))
                .thenReturn(Optional.of(delivery));
        when(fcmSender.send(anyList())).thenAnswer(invocation -> {
            List<SendNotificationCommand> commands = invocation.getArgument(0);
            return new SendBatchResult(
                    List.of(
                            new SendDetails(true, "message-001", null, null, commands.get(0)),
                            new SendDetails(false, null, "token unregistered", "UNREGISTERED", commands.get(1))
                    ),
                    1,
                    1
            );
        });
        when(fcmFailureClassifier.isInvalidToken(any(SendDetails.class))).thenReturn(true);
        when(tokenRepository.deleteByTokenIn(List.of("token-002")))
                .thenThrow(new IllegalStateException("db down"));

        service.notice(event, "event-001");

        verify(notificationDeliveryService).markSent(delivery);
        verify(notificationDeliveryService, never()).markFailed(any(NotificationDelivery.class), any());
    }

    @Test
    @DisplayName("FCM 발송 성공 후 성공 상태 저장에 실패하면 실패 delivery로 덮어쓰지 않고 예외를 전파한다")
    void notice_whenMarkSentFailsAfterFcmSuccess_propagatesWithoutMarkFailed() throws Throwable {
        PaymentEvent event = paymentEvent();
        NotificationDelivery delivery = delivery("event-001", "user-001");

        when(notificationService.canSend("user-001", NotificationType.PAYMENT)).thenReturn(true);
        when(tokenRepository.findAllTokensByUserId("user-001")).thenReturn(List.of("token-001"));
        when(notificationDeliveryService.claimPaymentFcmDelivery("event-001", "user-001"))
                .thenReturn(Optional.of(delivery));
        when(fcmSender.send(anyList())).thenReturn(new SendBatchResult(
                List.of(new SendDetails(true, "message-001", null, null, command("token-001", "user-001"))),
                1,
                0
        ));
        doThrow(new IllegalStateException("mark sent failed"))
                .when(notificationDeliveryService)
                .markSent(delivery);

        assertThatThrownBy(() -> service.notice(event, "event-001"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("mark sent failed");
        verify(notificationDeliveryService, never()).markFailed(any(NotificationDelivery.class), any());
    }

    @Test
    @DisplayName("bulk 알림에서 설정이 없거나 토큰이 없는 사용자는 재시도 없이 건너뛴다")
    @SuppressWarnings("unchecked")
    void noticeBulk_whenSettingOrTokenIsMissing_skipsWithoutRetryableException() throws Throwable {
        PaymentEvent tokenUserEvent = paymentEvent("user-001");
        PaymentEvent noTokenUserEvent = paymentEvent("user-002");
        PaymentEvent noSettingUserEvent = paymentEvent("user-003");
        TokenEntity token = new TokenEntity("token-001", "user-001");

        when(notificationService.canSend(
                List.of("user-001", "user-002", "user-003"),
                NotificationType.PAYMENT
        )).thenReturn(Map.of(
                "user-001", true,
                "user-002", true
        ));
        when(tokenRepository.findAllByUserIdIn(List.of("user-001", "user-002"))).thenReturn(List.of(token));
        when(notificationDeliveryService.claimPaymentFcmDelivery("event-001", "user-001"))
                .thenReturn(Optional.of(delivery("event-001", "user-001")));
        when(fcmSender.send(anyList())).thenReturn(new SendBatchResult(List.of(), 0, 0));

        service.notice(List.of(tokenUserEvent, noTokenUserEvent, noSettingUserEvent), "event-001");

        ArgumentCaptor<List<SendNotificationCommand>> captor = ArgumentCaptor.forClass(List.class);
        verify(fcmSender).send(captor.capture());
        assertThat(captor.getValue()).hasSize(1);
        assertThat(captor.getValue().getFirst().target()).isEqualTo("token-001");
        assertThat(captor.getValue().getFirst().data()).containsEntry("eventId", "event-001");
        assertThat(captor.getValue().getFirst().data()).containsEntry("dedupKey", "PAYMENT_COMPLETED:order-001");
    }

    @Test
    @DisplayName("bulk 알림에서 발송 대상 토큰이 하나도 없으면 FCM 요청을 보내지 않는다")
    void noticeBulk_whenNoTokens_skipsFcmRequest() throws Throwable {
        PaymentEvent event = paymentEvent("user-001");

        when(notificationService.canSend(List.of("user-001"), NotificationType.PAYMENT))
                .thenReturn(Map.of("user-001", true));
        when(tokenRepository.findAllByUserIdIn(List.of("user-001"))).thenReturn(List.of());

        service.notice(List.of(event), "event-001");

        verify(fcmSender, never()).send(anyList());
    }

    @Test
    @DisplayName("bulk 알림에서 이미 claim된 사용자는 제외하고 새 사용자만 발송한다")
    @SuppressWarnings("unchecked")
    void noticeBulk_whenSomeDeliveriesAlreadyClaimed_sendsOnlyNewUsers() throws Throwable {
        PaymentEvent claimedEvent = paymentEvent("user-001");
        PaymentEvent newEvent = paymentEvent("user-002");
        TokenEntity claimedToken = new TokenEntity("token-001", "user-001");
        TokenEntity newToken = new TokenEntity("token-002", "user-002");
        NotificationDelivery newDelivery = delivery("event-001", "user-002");

        when(notificationService.canSend(List.of("user-001", "user-002"), NotificationType.PAYMENT))
                .thenReturn(Map.of("user-001", true, "user-002", true));
        when(tokenRepository.findAllByUserIdIn(List.of("user-001", "user-002")))
                .thenReturn(List.of(claimedToken, newToken));
        when(notificationDeliveryService.claimPaymentFcmDelivery("event-001", "user-001"))
                .thenReturn(Optional.empty());
        when(notificationDeliveryService.claimPaymentFcmDelivery("event-001", "user-002"))
                .thenReturn(Optional.of(newDelivery));
        when(fcmSender.send(anyList())).thenReturn(new SendBatchResult(
                List.of(new SendDetails(true, "message-001", null, null, command("token-002", "user-002"))),
                1,
                0
        ));

        service.notice(List.of(claimedEvent, newEvent), "event-001");

        ArgumentCaptor<List<SendNotificationCommand>> captor = ArgumentCaptor.forClass(List.class);
        verify(fcmSender).send(captor.capture());
        assertThat(captor.getValue()).hasSize(1);
        assertThat(captor.getValue().getFirst().target()).isEqualTo("token-002");
        verify(notificationDeliveryService).markSent(newDelivery);
        verify(notificationDeliveryService, never()).markFailed(any(NotificationDelivery.class), any());
    }

    @Test
    @DisplayName("bulk 알림 결과를 사용자별로 집계해 성공 사용자는 성공, 실패 사용자만 실패로 기록한다")
    void noticeBulk_whenResultsAreMixed_marksDeliveryByUserResult() throws Throwable {
        PaymentEvent successEvent = paymentEvent("user-001");
        PaymentEvent failedEvent = paymentEvent("user-002");
        TokenEntity successToken = new TokenEntity("token-001", "user-001");
        TokenEntity partialFailureToken = new TokenEntity("token-002", "user-001");
        TokenEntity failedToken = new TokenEntity("token-003", "user-002");
        NotificationDelivery successDelivery = delivery("event-001", "user-001");
        NotificationDelivery failedDelivery = delivery("event-001", "user-002");

        when(notificationService.canSend(List.of("user-001", "user-002"), NotificationType.PAYMENT))
                .thenReturn(Map.of("user-001", true, "user-002", true));
        when(tokenRepository.findAllByUserIdIn(List.of("user-001", "user-002")))
                .thenReturn(List.of(successToken, partialFailureToken, failedToken));
        when(notificationDeliveryService.claimPaymentFcmDelivery("event-001", "user-001"))
                .thenReturn(Optional.of(successDelivery));
        when(notificationDeliveryService.claimPaymentFcmDelivery("event-001", "user-002"))
                .thenReturn(Optional.of(failedDelivery));
        when(fcmSender.send(anyList())).thenReturn(new SendBatchResult(
                List.of(
                        new SendDetails(true, "message-001", null, null, command("token-001", "user-001")),
                        new SendDetails(false, null, "token expired", null, command("token-002", "user-001")),
                        new SendDetails(false, null, "fcm down", null, command("token-003", "user-002"))
                ),
                1,
                2
        ));

        service.notice(List.of(successEvent, failedEvent), "event-001");

        verify(notificationDeliveryService).markSent(successDelivery);
        verify(notificationDeliveryService).markFailed(failedDelivery, "fcm down");
    }

    @Test
    @DisplayName("bulk 알림에서 invalid token 응답만 토큰 정리 대상으로 기록한다")
    void noticeBulk_whenResultContainsInvalidToken_deletesOnlyInvalidTokens() throws Throwable {
        PaymentEvent event = paymentEvent("user-001");
        TokenEntity validToken = new TokenEntity("token-001", "user-001");
        TokenEntity invalidToken = new TokenEntity("token-002", "user-001");
        NotificationDelivery delivery = delivery("event-001", "user-001");

        when(notificationService.canSend(List.of("user-001"), NotificationType.PAYMENT))
                .thenReturn(Map.of("user-001", true));
        when(tokenRepository.findAllByUserIdIn(List.of("user-001")))
                .thenReturn(List.of(validToken, invalidToken));
        when(notificationDeliveryService.claimPaymentFcmDelivery("event-001", "user-001"))
                .thenReturn(Optional.of(delivery));
        when(fcmSender.send(anyList())).thenAnswer(invocation -> {
            List<SendNotificationCommand> commands = invocation.getArgument(0);
            return new SendBatchResult(
                    List.of(
                            new SendDetails(true, "message-001", null, null, commands.get(0)),
                            new SendDetails(false, null, "token unregistered", "UNREGISTERED", commands.get(1))
                    ),
                    1,
                    1
            );
        });
        when(fcmFailureClassifier.isInvalidToken(any(SendDetails.class))).thenAnswer(invocation -> {
            SendDetails details = invocation.getArgument(0);
            return "UNREGISTERED".equals(details.errorCode());
        });

        service.notice(List.of(event), "event-001");

        verify(tokenRepository).deleteByTokenIn(List.of("token-002"));
        verify(notificationDeliveryService).markSent(delivery);
        verify(notificationDeliveryService, never()).markFailed(any(NotificationDelivery.class), any());
    }

    @Test
    @DisplayName("bulk FCM 발송 성공 후 상태 저장에 실패하면 전체 delivery를 실패로 덮어쓰지 않고 예외를 전파한다")
    void noticeBulk_whenMarkSentFailsAfterFcmSuccess_propagatesWithoutMarkingAllFailed() throws Throwable {
        PaymentEvent event = paymentEvent("user-001");
        TokenEntity token = new TokenEntity("token-001", "user-001");
        NotificationDelivery delivery = delivery("event-001", "user-001");

        when(notificationService.canSend(List.of("user-001"), NotificationType.PAYMENT))
                .thenReturn(Map.of("user-001", true));
        when(tokenRepository.findAllByUserIdIn(List.of("user-001"))).thenReturn(List.of(token));
        when(notificationDeliveryService.claimPaymentFcmDelivery("event-001", "user-001"))
                .thenReturn(Optional.of(delivery));
        when(fcmSender.send(anyList())).thenReturn(new SendBatchResult(
                List.of(new SendDetails(true, "message-001", null, null, command("token-001", "user-001"))),
                1,
                0
        ));
        doThrow(new IllegalStateException("mark sent failed"))
                .when(notificationDeliveryService)
                .markSent(delivery);

        assertThatThrownBy(() -> service.notice(List.of(event), "event-001"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("mark sent failed");
        verify(notificationDeliveryService, never()).markFailed(any(NotificationDelivery.class), any());
    }

    private NotificationDelivery delivery(String eventId, String userId) {
        return new NotificationDelivery(
                eventId,
                userId,
                NotificationType.PAYMENT,
                ChannelType.FCM,
                LocalDateTime.now()
        );
    }

    private SendNotificationCommand command(String token, String userId) {
        return new SendNotificationCommand(
                token,
                "결제 완료 안내",
                "body",
                Map.of("userId", userId),
                null
        );
    }

    private PaymentEvent paymentEvent() throws Exception {
        return paymentEvent("user-001");
    }

    private PaymentEvent paymentEvent(String userId) {
        try {
            return new ObjectMapper().readValue("""
                    {
                      "user_id": "%s",
                      "user_name": "홍길동",
                      "action": "PAYMENT_COMPLETED",
                      "amount": "10000",
                      "order_id": "order-001",
                      "timestamp": "2026-08-04T12:00:00",
                      "value": "lesson"
                    }
                    """.formatted(userId), PaymentEvent.class);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
