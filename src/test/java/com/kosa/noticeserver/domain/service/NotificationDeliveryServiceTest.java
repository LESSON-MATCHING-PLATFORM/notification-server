package com.kosa.noticeserver.domain.service;

import com.kosa.noticeserver.domain.model.NotificationDelivery;
import com.kosa.noticeserver.domain.model.ChannelType;
import com.kosa.noticeserver.domain.model.NotificationDeliveryStatus;
import com.kosa.noticeserver.domain.model.NotificationType;
import com.kosa.noticeserver.infrastructure.repository.NotificationDeliveryRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NotificationDeliveryServiceTest {

    private final NotificationDeliveryClaimer notificationDeliveryClaimer = mock(NotificationDeliveryClaimer.class);
    private final NotificationDeliveryRepository notificationDeliveryRepository = mock(NotificationDeliveryRepository.class);
    private final NotificationDeliveryService notificationDeliveryService = new NotificationDeliveryService(
            notificationDeliveryClaimer,
            notificationDeliveryRepository
    );

    @Test
    @DisplayName("eventId와 사용자 기준으로 FCM 결제 알림 delivery를 claim한다")
    void claimPaymentFcmDelivery_savesDelivery() {
        when(notificationDeliveryClaimer.claim(any(NotificationDelivery.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Optional<NotificationDelivery> claimed = notificationDeliveryService.claimPaymentFcmDelivery(
                "event-001",
                "user-001"
        );

        assertThat(claimed).isPresent();
        assertThat(claimed.get().getEventId()).isEqualTo("event-001");
        assertThat(claimed.get().getUserId()).isEqualTo("user-001");
        assertThat(claimed.get().getStatus()).isEqualTo(NotificationDeliveryStatus.CLAIMED);
    }

    @Test
    @DisplayName("unique key 충돌 후 reclaim 대상이 아니면 중복 delivery로 판단하고 empty를 반환한다")
    void claimPaymentFcmDelivery_whenDuplicateIsNotReclaimable_returnsEmpty() {
        when(notificationDeliveryClaimer.claim(any(NotificationDelivery.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate"));
        when(notificationDeliveryClaimer.reclaimPaymentFcmDelivery(any(), any(), any(), any()))
                .thenReturn(Optional.empty());

        Optional<NotificationDelivery> claimed = notificationDeliveryService.claimPaymentFcmDelivery(
                "event-001",
                "user-001"
        );

        assertThat(claimed).isEmpty();
    }

    @Test
    @DisplayName("unique key 충돌 후 reclaim 대상이면 delivery를 반환한다")
    void claimPaymentFcmDelivery_whenDuplicateIsReclaimable_returnsReclaimedDelivery() {
        NotificationDelivery reclaimed = new NotificationDelivery(
                "event-001",
                "user-001",
                NotificationType.PAYMENT,
                ChannelType.FCM,
                LocalDateTime.now()
        );
        when(notificationDeliveryClaimer.claim(any(NotificationDelivery.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate"));
        when(notificationDeliveryClaimer.reclaimPaymentFcmDelivery(any(), any(), any(), any()))
                .thenReturn(Optional.of(reclaimed));

        Optional<NotificationDelivery> claimed = notificationDeliveryService.claimPaymentFcmDelivery(
                "event-001",
                "user-001"
        );

        assertThat(claimed).containsSame(reclaimed);
    }

    @Test
    @DisplayName("eventId가 비어 있으면 delivery를 저장하지 않는다")
    void claimPaymentFcmDelivery_whenEventIdIsBlank_returnsEmpty() {
        Optional<NotificationDelivery> claimed = notificationDeliveryService.claimPaymentFcmDelivery(
                " ",
                "user-001"
        );

        assertThat(claimed).isEmpty();
        verify(notificationDeliveryClaimer, never()).claim(any());
    }
}
