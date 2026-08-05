package com.kosa.noticeserver.domain.service;

import com.kosa.noticeserver.domain.model.ChannelType;
import com.kosa.noticeserver.domain.model.NotificationDelivery;
import com.kosa.noticeserver.domain.model.NotificationDeliveryStatus;
import com.kosa.noticeserver.domain.model.NotificationType;
import com.kosa.noticeserver.infrastructure.repository.NotificationDeliveryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationDeliveryService {

    static final Duration CLAIMED_RECOVERY_THRESHOLD = Duration.ofMinutes(10);

    private final NotificationDeliveryClaimer notificationDeliveryClaimer;
    private final NotificationDeliveryRepository notificationDeliveryRepository;

    public Optional<NotificationDelivery> claimPaymentFcmDelivery(String eventId, String userId) {
        if (!StringUtils.hasText(eventId)) {
            log.error("Notification delivery eventId is blank. userId={}", userId);
            return Optional.empty();
        }

        try {
            LocalDateTime now = LocalDateTime.now();
            NotificationDelivery delivery = new NotificationDelivery(
                    eventId,
                    userId,
                    NotificationType.PAYMENT,
                    ChannelType.FCM,
                    now
            );
            return Optional.of(notificationDeliveryClaimer.claim(delivery));
        } catch (DataIntegrityViolationException e) {
            LocalDateTime now = LocalDateTime.now();
            Optional<NotificationDelivery> reclaimed = notificationDeliveryClaimer.reclaimPaymentFcmDelivery(
                    eventId,
                    userId,
                    now.minus(CLAIMED_RECOVERY_THRESHOLD),
                    now
            );
            if (reclaimed.isPresent()) {
                log.info("Notification delivery reclaimed. eventId={}, userId={}", eventId, userId);
                return reclaimed;
            }

            log.info("Duplicate active notification delivery skipped. eventId={}, userId={}", eventId, userId);
            return Optional.empty();
        }
    }

    public long countStaleClaimedDeliveries() {
        return notificationDeliveryRepository.countByStatusAndClaimedAtBefore(
                NotificationDeliveryStatus.CLAIMED,
                LocalDateTime.now().minus(CLAIMED_RECOVERY_THRESHOLD)
        );
    }

    @Transactional
    public void markSent(NotificationDelivery delivery) {
        delivery.markSent(LocalDateTime.now());
        notificationDeliveryRepository.save(delivery);
    }

    @Transactional
    public void markFailed(NotificationDelivery delivery, String errorMessage) {
        delivery.markFailed(LocalDateTime.now(), errorMessage);
        notificationDeliveryRepository.save(delivery);
    }
}
