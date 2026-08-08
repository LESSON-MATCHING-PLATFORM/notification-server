package com.kosa.noticeserver.config;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class KafkaFailureLogContextTest {

    @Test
    @DisplayName("retry exhausted 관측에 필요한 Kafka record와 예외 정보를 추출한다")
    void from_extractsRecordAndExceptionContext() {
        ConsumerRecord<String, String> record = new ConsumerRecord<>(
                "payment-topic",
                1,
                42L,
                "order-001",
                "payload"
        );
        record.headers().add("eventId", "event-001".getBytes(StandardCharsets.UTF_8));

        KafkaFailureLogContext context = KafkaFailureLogContext.from(
                record,
                new IllegalStateException("database down")
        );

        assertThat(context.topic()).isEqualTo("payment-topic");
        assertThat(context.partition()).isEqualTo(1);
        assertThat(context.offset()).isEqualTo(42L);
        assertThat(context.key()).isEqualTo("order-001");
        assertThat(context.eventId()).isEqualTo("event-001");
        assertThat(context.exceptionClass()).isEqualTo(IllegalStateException.class.getName());
        assertThat(context.exceptionMessage()).isEqualTo("database down");
    }

    @Test
    @DisplayName("eventId header가 없어도 retry exhausted 관측 컨텍스트를 만들 수 있다")
    void from_whenEventIdHeaderIsMissing_returnsNullEventId() {
        ConsumerRecord<String, String> record = new ConsumerRecord<>(
                "payment-topic",
                0,
                10L,
                null,
                "payload"
        );

        KafkaFailureLogContext context = KafkaFailureLogContext.from(record, new RuntimeException());

        assertThat(context.eventId()).isNull();
    }
}
