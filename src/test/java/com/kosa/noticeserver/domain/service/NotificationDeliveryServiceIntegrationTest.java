package com.kosa.noticeserver.domain.service;

import com.kosa.noticeserver.domain.model.NotificationDelivery;
import com.kosa.noticeserver.infrastructure.repository.NotificationDeliveryRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

@SpringBootTest(properties = {
        "spring.config.import=",
        "spring.kafka.listener.auto-startup=false"
})
@Testcontainers
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class NotificationDeliveryServiceIntegrationTest {

    @Container
    @ServiceConnection
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.4");

    @Autowired
    private NotificationDeliveryService notificationDeliveryService;

    @Autowired
    private NotificationDeliveryRepository notificationDeliveryRepository;

    @Test
    @DisplayName("중복 delivery claim은 실제 DB unique key 충돌 후에도 empty로 처리하고 rollback 예외를 전파하지 않는다")
    void claimPaymentFcmDelivery_whenDuplicateWithRealDatabase_returnsEmptyWithoutUnexpectedRollback() {
        Optional<NotificationDelivery> firstClaim = notificationDeliveryService.claimPaymentFcmDelivery(
                "event-001",
                "user-001"
        );

        assertThat(firstClaim).isPresent();

        assertThatCode(() -> {
            Optional<NotificationDelivery> duplicatedClaim = notificationDeliveryService.claimPaymentFcmDelivery(
                    "event-001",
                    "user-001"
            );

            assertThat(duplicatedClaim).isEmpty();
        }).doesNotThrowAnyException();

        assertThat(notificationDeliveryRepository.count()).isEqualTo(1);
    }
}
