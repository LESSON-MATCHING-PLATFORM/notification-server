package com.kosa.noticeserver.consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kosa.noticeserver.domain.model.event.PaymentEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class TestTopicConsumer {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @KafkaListener(topics = "test-topic", groupId = "notification-group")
    public void consume(String payload) {
        log.info("Raw Payload received: {}", payload);

        try {
            // 1. String을 클래스 객체로 변환
            PaymentEvent event = objectMapper.readValue(payload, PaymentEvent.class);

            // 2. 비즈니스 로직 수행
            log.info("Success to deserialize: {}", event);
//            processNotification(event);

        } catch (JsonProcessingException e) {
            // 3. 역직렬화 실패 시 예외 처리 (Dead Letter Topic 등으로 보낼 수 있음)
            log.error("Failed to parse JSON: {}. Error: {}", payload, e.getMessage());
        }
    }

}
