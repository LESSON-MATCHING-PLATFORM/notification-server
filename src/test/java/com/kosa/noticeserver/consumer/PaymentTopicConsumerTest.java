package com.kosa.noticeserver.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kosa.noticeserver.domain.model.EventType;
import com.kosa.noticeserver.domain.model.event.PaymentEvent;
import com.kosa.noticeserver.domain.service.MetadataService;
import com.kosa.noticeserver.domain.service.PaymentEventNotificationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class PaymentTopicConsumerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final PaymentEventNotificationService notificationService = mock(PaymentEventNotificationService.class);
    private final MetadataService metadataService = mock(MetadataService.class);
    private final PaymentTopicConsumer consumer = new PaymentTopicConsumer(objectMapper, notificationService, metadataService);

    @Test
    @DisplayName("잘못된 JSON payload는 알림 처리 없이 소비한다")
    void consumePaymentEvent_whenPayloadIsInvalid_doesNotRetry() {
        consumer.consumePaymentEvent("{invalid", "event-001");

        verify(metadataService).recordConsumedEvent(EventType.PAYMENT, "event-001", "{invalid");
        verify(notificationService, never()).notice(any(PaymentEvent.class), any());
    }

    @Test
    @DisplayName("null payment payload는 알림 처리 없이 소비한다")
    void consumePaymentEvent_whenPayloadIsNull_doesNotRetry() {
        assertThatCode(() -> consumer.consumePaymentEvent(null, "event-001"))
                .doesNotThrowAnyException();

        verify(metadataService).recordConsumedEvent(EventType.PAYMENT, "event-001", null);
        verify(notificationService, never()).notice(any(PaymentEvent.class), any());
    }

    @Test
    @DisplayName("잘못된 bulk JSON payload는 알림 처리 없이 소비한다")
    void consumePaymentBulkEvent_whenPayloadIsInvalid_doesNotRetry() {
        consumer.consumePaymentBulkEvent("{invalid", "event-001");

        verify(metadataService).recordConsumedEvent(EventType.PAYMENT_BULK, "event-001", "{invalid");
        verify(notificationService, never()).notice(anyList(), any());
    }

    @Test
    @DisplayName("null payment bulk payload는 알림 처리 없이 소비한다")
    void consumePaymentBulkEvent_whenPayloadIsNull_doesNotRetry() {
        assertThatCode(() -> consumer.consumePaymentBulkEvent(null, "event-001"))
                .doesNotThrowAnyException();

        verify(metadataService).recordConsumedEvent(EventType.PAYMENT_BULK, "event-001", null);
        verify(notificationService, never()).notice(anyList(), any());
    }

    @Test
    @DisplayName("알림 처리 중 예상하지 못한 예외는 listener 밖으로 전파한다")
    void consumePaymentEvent_whenUnexpectedNotificationFailure_propagatesException() {
        doThrow(new IllegalStateException("database down"))
                .when(notificationService)
                .notice(any(PaymentEvent.class), eq("event-001"));

        assertThatThrownBy(() -> consumer.consumePaymentEvent(paymentPayload(), "event-001"))
                .isInstanceOf(IllegalStateException.class);

        verify(metadataService).recordConsumedEvent(EventType.PAYMENT, "event-001", paymentPayload());
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
