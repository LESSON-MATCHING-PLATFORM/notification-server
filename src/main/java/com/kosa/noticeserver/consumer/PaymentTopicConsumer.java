package com.kosa.noticeserver.consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kosa.noticeserver.domain.model.event.PaymentBulkEvent;
import com.kosa.noticeserver.domain.model.event.PaymentEvent;
import com.kosa.noticeserver.domain.service.PaymentEventNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jboss.logging.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentTopicConsumer {

    private final ObjectMapper objectMapper;
    private final PaymentEventNotificationService paymentEventNotificationService;

    @KafkaListener(topics = "payment-topic", groupId = "notification-group")
    public void consumePaymentEvent(
            String payload,
            @Header(name = "eventId") String eventId
    ) {
        log.info("Payment Event Received");

        MDC.put("eventId", eventId);

        try {
            PaymentEvent paymentEvent = objectMapper.readValue(payload, PaymentEvent.class);

            log.info("Received Payment Event {}", paymentEvent);

            paymentEventNotificationService.notice(paymentEvent, eventId);
        } catch (JsonProcessingException jsonProcessingException) {
            log.error("Failed to parse JSON: {}. Error: {}", payload, jsonProcessingException.getMessage());
        } finally {
            MDC.clear();
        }
    }

    @KafkaListener(topics = "payment-bulk-topic", groupId = "notification-group")
    public void consumePaymentBulkEvent(
            String payload,
            @Header(name = "eventId") String eventId
    ) {
        log.info("Payment Bulk Event Received");

        MDC.put("eventId", eventId);

        try {
            PaymentBulkEvent paymentBulkEvent = objectMapper.readValue(payload, PaymentBulkEvent.class);

            log.info("Received Payment Bulk Event {}", paymentBulkEvent);

            paymentEventNotificationService.notice(paymentBulkEvent.getEvents(), eventId);
        } catch (JsonProcessingException jsonProcessingException) {
            log.error("Failed to parse JSON: {}. Error: {}", payload, jsonProcessingException.getMessage());
        }  finally {
            MDC.clear();
        }
    }
}
