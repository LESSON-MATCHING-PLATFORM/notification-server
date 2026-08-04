package com.kosa.noticeserver.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kosa.noticeserver.domain.model.event.PaymentEvent;
import com.kosa.noticeserver.domain.service.NotificationDeliveryException;
import com.kosa.noticeserver.domain.service.PaymentEventNotificationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class PaymentTopicConsumerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final PaymentEventNotificationService notificationService = mock(PaymentEventNotificationService.class);
    private final PaymentTopicConsumer consumer = new PaymentTopicConsumer(objectMapper, notificationService);

    @Test
    @DisplayName("잘못된 JSON payload는 알림 처리 없이 소비한다")
    void consumePaymentEvent_whenPayloadIsInvalid_doesNotRetry() {
        consumer.consumePaymentEvent("{invalid", "event-001");

        verify(notificationService, never()).notice(any(PaymentEvent.class));
    }

    @Test
    @DisplayName("알림 처리 실패는 Kafka error handler 재시도를 위해 예외를 전파한다")
    void consumePaymentEvent_whenNotificationFails_propagatesException() {
        doThrow(new NotificationDeliveryException("temporary fcm failure", new RuntimeException("fcm down")))
                .when(notificationService)
                .notice(any(PaymentEvent.class));

        assertThatThrownBy(() -> consumer.consumePaymentEvent(paymentPayload(), "event-001"))
                .isInstanceOf(NotificationDeliveryException.class);
    }

    private String paymentPayload() {
        return """
                {
                  "user_id": "user-001",
                  "user_name": "홍길동",
                  "action": "PAYMENT_COMPLETED",
                  "amount": "10000",
                  "order_id": "order-001",
                  "timestamp": "2026-08-04T12:00:00",
                  "value": "lesson"
                }
                """;
    }
}
