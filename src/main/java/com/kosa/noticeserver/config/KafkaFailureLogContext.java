package com.kosa.noticeserver.config;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;

import java.nio.charset.StandardCharsets;

record KafkaFailureLogContext(
        String topic,
        int partition,
        long offset,
        Object key,
        String eventId,
        String exceptionClass,
        String exceptionMessage
) {

    static KafkaFailureLogContext from(ConsumerRecord<?, ?> record, Exception exception) {
        return new KafkaFailureLogContext(
                record.topic(),
                record.partition(),
                record.offset(),
                record.key(),
                headerValue(record, "eventId"),
                exceptionClass(exception),
                exceptionMessage(exception)
        );
    }

    private static String headerValue(ConsumerRecord<?, ?> record, String name) {
        Header header = record.headers().lastHeader(name);
        if (header == null || header.value() == null) {
            return null;
        }
        return new String(header.value(), StandardCharsets.UTF_8);
    }

    private static String exceptionClass(Exception exception) {
        if (exception == null) {
            return null;
        }
        return exception.getClass().getName();
    }

    private static String exceptionMessage(Exception exception) {
        if (exception == null) {
            return null;
        }
        return exception.getMessage();
    }
}
