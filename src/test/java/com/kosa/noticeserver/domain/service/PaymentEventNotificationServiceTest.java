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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
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
    private final PaymentEventNotificationService service = new PaymentEventNotificationService(
            tokenRepository,
            fcmSender,
            notificationService,
            notificationDeliveryService
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
    @DisplayName("같은 eventId와 사용자 조합을 두 번 처리해도 FCM은 한 번만 발송한다")
    void notice_whenSameEventAndUserIsConsumedTwice_sendsFcmOnce() throws Throwable {
        PaymentEvent event = paymentEvent();

        when(notificationService.canSend("user-001", NotificationType.PAYMENT)).thenReturn(true);
        when(tokenRepository.findAllTokensByUserId("user-001")).thenReturn(List.of("token-001"));
        when(notificationDeliveryService.claimPaymentFcmDelivery("event-001", "user-001"))
                .thenReturn(Optional.of(delivery("event-001", "user-001")))
                .thenReturn(Optional.empty());
        when(fcmSender.send(anyList())).thenReturn(new SendBatchResult(
                List.of(new SendDetails(true, "message-001", null, null, command("token-001", "user-001"))),
                1,
                0
        ));

        service.notice(event, "event-001");
        service.notice(event, "event-001");

        verify(fcmSender, times(1)).send(anyList());
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

        when(notificationService.canSend(List.of("user-001", "user-002"), NotificationType.PAYMENT))
                .thenReturn(Map.of("user-001", true, "user-002", true));
        when(tokenRepository.findAllByUserIdIn(List.of("user-001", "user-002")))
                .thenReturn(List.of(claimedToken, newToken));
        when(notificationDeliveryService.claimPaymentFcmDelivery("event-001", "user-001"))
                .thenReturn(Optional.empty());
        when(notificationDeliveryService.claimPaymentFcmDelivery("event-001", "user-002"))
                .thenReturn(Optional.of(delivery("event-001", "user-002")));
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
