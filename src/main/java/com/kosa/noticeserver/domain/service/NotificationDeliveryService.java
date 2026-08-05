package com.kosa.noticeserver.domain.service;

import com.kosa.noticeserver.domain.model.ChannelType;
import com.kosa.noticeserver.domain.model.NotificationDelivery;
import com.kosa.noticeserver.domain.model.NotificationType;
import com.kosa.noticeserver.infrastructure.repository.NotificationDeliveryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationDeliveryService {

    private final NotificationDeliveryClaimer notificationDeliveryClaimer;
    private final NotificationDeliveryRepository notificationDeliveryRepository;

    public Optional<NotificationDelivery> claimPaymentFcmDelivery(String eventId, String userId) {
        if (!StringUtils.hasText(eventId)) {
            log.error("Notification delivery eventId is blank. userId={}", userId);
            return Optional.empty();
        }

        try {
            NotificationDelivery delivery = new NotificationDelivery(
                    eventId,
                    userId,
                    NotificationType.PAYMENT,
                    ChannelType.FCM,
                    LocalDateTime.now()
            );
            return Optional.of(notificationDeliveryClaimer.claim(delivery));
        } catch (DataIntegrityViolationException e) {
            log.info("Duplicate notification delivery skipped. eventId={}, userId={}", eventId, userId);
            return Optional.empty();
        }
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
