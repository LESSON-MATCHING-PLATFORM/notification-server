package com.kosa.noticeserver.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
@Slf4j
public class KafkaConsumerRetryConfig {

    static final long RETRY_INTERVAL_MS = 1_000L;
    static final long MAX_RETRY_ATTEMPTS = 2L;

    @Bean
    public DefaultErrorHandler kafkaDefaultErrorHandler() {
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(
                this::logExhaustedRecord,
                new FixedBackOff(RETRY_INTERVAL_MS, MAX_RETRY_ATTEMPTS)
        );

        errorHandler.addNotRetryableExceptions(IllegalArgumentException.class);
        return errorHandler;
    }

    private void logExhaustedRecord(ConsumerRecord<?, ?> record, Exception exception) {
        KafkaFailureLogContext context = KafkaFailureLogContext.from(record, exception);

        log.error(
                "Kafka consumer retry exhausted. topic={}, partition={}, offset={}, key={}, eventId={}, exceptionClass={}, exceptionMessage={}",
                context.topic(),
                context.partition(),
                context.offset(),
                context.key(),
                context.eventId(),
                context.exceptionClass(),
                context.exceptionMessage(),
                exception
        );
    }
}
