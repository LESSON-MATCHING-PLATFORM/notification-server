package com.kosa.noticeserver.consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kosa.noticeserver.domain.model.Event;
import com.kosa.noticeserver.domain.model.EventType;
import com.kosa.noticeserver.domain.model.event.PaymentBulkEvent;
import com.kosa.noticeserver.domain.model.event.PaymentEvent;
import com.kosa.noticeserver.domain.service.PaymentEventNotificationService;
import com.kosa.noticeserver.infrastructure.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentTopicConsumer {

    private final ObjectMapper objectMapper;
    private final PaymentEventNotificationService paymentEventNotificationService;
    private final EventRepository eventRepository;

    @KafkaListener(topics = "payment-topic", groupId = "notification-group")
    public void consumePaymentEvent(String payload) {
        log.info("Payment Event Received");

        try {
            PaymentEvent paymentEvent = objectMapper.readValue(payload, PaymentEvent.class);

            eventRepository.save(
                    new Event(
                        EventType.PAYMENT,
                        paymentEvent.getId(),
                        LocalDateTime.now(),
                        payload
                    )
            );

            paymentEventNotificationService.notice(paymentEvent);
        } catch (JsonProcessingException jsonProcessingException) {
            log.error("Failed to parse JSON: {}. Error: {}", payload, jsonProcessingException.getMessage());
        }
    }

    @KafkaListener(topics = "payment-bulk-topic", groupId = "notification-group")
    public void consumePaymentBulkEvent(String payload) {
        log.info("Payment Bulk Event Received");

        try {
            PaymentBulkEvent paymentBulkEvent = objectMapper.readValue(payload, PaymentBulkEvent.class);

            eventRepository.save(
                    new Event(
                            EventType.PAYMENT_BULK,
                            paymentBulkEvent.getBatchId(),
                            LocalDateTime.now(),
                            payload
                    )
            );

            paymentEventNotificationService.notice(paymentBulkEvent.getEvents());
        } catch (JsonProcessingException jsonProcessingException) {
            log.error("Failed to parse JSON: {}. Error: {}", payload, jsonProcessingException.getMessage());
        }
    }
}
