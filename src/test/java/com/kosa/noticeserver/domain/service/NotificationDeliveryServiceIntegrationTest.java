package com.kosa.noticeserver.domain.service;

import com.kosa.noticeserver.domain.model.NotificationDelivery;
import com.kosa.noticeserver.domain.model.ChannelType;
import com.kosa.noticeserver.domain.model.NotificationDeliveryStatus;
import com.kosa.noticeserver.domain.model.NotificationType;
import com.kosa.noticeserver.infrastructure.repository.NotificationDeliveryRepository;
import org.junit.jupiter.api.BeforeEach;
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

import java.time.LocalDateTime;
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

    @BeforeEach
    void setUp() {
        notificationDeliveryRepository.deleteAllInBatch();
    }

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

    @Test
    @DisplayName("FAILED delivery는 중복 claim 시 다시 CLAIMED로 전환된다")
    void claimPaymentFcmDelivery_whenDeliveryFailed_reclaimsDelivery() {
        NotificationDelivery failed = notificationDeliveryService.claimPaymentFcmDelivery(
                "event-failed",
                "user-001"
        ).orElseThrow();
        notificationDeliveryService.markFailed(failed, "fcm down");

        Optional<NotificationDelivery> reclaimed = notificationDeliveryService.claimPaymentFcmDelivery(
                "event-failed",
                "user-001"
        );

        assertThat(reclaimed).isPresent();
        assertThat(reclaimed.get().getStatus()).isEqualTo(NotificationDeliveryStatus.CLAIMED);
        assertThat(reclaimed.get().getCompletedAt()).isNull();
        assertThat(reclaimed.get().getLastError()).isNull();
        assertThat(notificationDeliveryRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("오래된 CLAIMED delivery는 중복 claim 시 다시 CLAIMED로 갱신된다")
    void claimPaymentFcmDelivery_whenClaimedIsStale_reclaimsDelivery() {
        NotificationDelivery staleClaimed = new NotificationDelivery(
                "event-stale",
                "user-001",
                NotificationType.PAYMENT,
                ChannelType.FCM,
                LocalDateTime.now().minus(NotificationDeliveryService.CLAIMED_RECOVERY_THRESHOLD).minusSeconds(1)
        );
        notificationDeliveryRepository.saveAndFlush(staleClaimed);

        Optional<NotificationDelivery> reclaimed = notificationDeliveryService.claimPaymentFcmDelivery(
                "event-stale",
                "user-001"
        );

        assertThat(reclaimed).isPresent();
        assertThat(reclaimed.get().getStatus()).isEqualTo(NotificationDeliveryStatus.CLAIMED);
        assertThat(reclaimed.get().getClaimedAt()).isAfter(staleClaimed.getClaimedAt());
        assertThat(notificationDeliveryRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("아직 오래되지 않은 CLAIMED delivery는 중복 claim 시 empty로 처리한다")
    void claimPaymentFcmDelivery_whenClaimedIsFresh_returnsEmpty() {
        NotificationDelivery freshClaimed = new NotificationDelivery(
                "event-fresh",
                "user-001",
                NotificationType.PAYMENT,
                ChannelType.FCM,
                LocalDateTime.now()
        );
        notificationDeliveryRepository.saveAndFlush(freshClaimed);

        Optional<NotificationDelivery> duplicatedClaim = notificationDeliveryService.claimPaymentFcmDelivery(
                "event-fresh",
                "user-001"
        );

        assertThat(duplicatedClaim).isEmpty();
        assertThat(notificationDeliveryRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("오래된 CLAIMED delivery 수를 운영 지표로 확인할 수 있다")
    void countStaleClaimedDeliveries_countsOnlyStaleClaimed() {
        notificationDeliveryRepository.saveAndFlush(new NotificationDelivery(
                "event-stale-count",
                "user-001",
                NotificationType.PAYMENT,
                ChannelType.FCM,
                LocalDateTime.now().minus(NotificationDeliveryService.CLAIMED_RECOVERY_THRESHOLD).minusSeconds(1)
        ));
        notificationDeliveryRepository.saveAndFlush(new NotificationDelivery(
                "event-fresh-count",
                "user-001",
                NotificationType.PAYMENT,
                ChannelType.FCM,
                LocalDateTime.now()
        ));
        NotificationDelivery failed = notificationDeliveryService.claimPaymentFcmDelivery(
                "event-failed-count",
                "user-001"
        ).orElseThrow();
        notificationDeliveryService.markFailed(failed, "fcm down");

        assertThat(notificationDeliveryService.countStaleClaimedDeliveries()).isEqualTo(1);
    }
}
