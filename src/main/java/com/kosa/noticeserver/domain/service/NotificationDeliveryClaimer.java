package com.kosa.noticeserver.domain.service;

import com.kosa.noticeserver.domain.model.ChannelType;
import com.kosa.noticeserver.domain.model.NotificationDelivery;
import com.kosa.noticeserver.domain.model.NotificationType;
import com.kosa.noticeserver.infrastructure.repository.NotificationDeliveryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class NotificationDeliveryClaimer {

    private final NotificationDeliveryRepository notificationDeliveryRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public NotificationDelivery claim(NotificationDelivery delivery) {
        return notificationDeliveryRepository.saveAndFlush(delivery);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Optional<NotificationDelivery> reclaimPaymentFcmDelivery(
            String eventId,
            String userId,
            LocalDateTime staleBefore,
            LocalDateTime claimedAt
    ) {
        int updated = notificationDeliveryRepository.reclaimPaymentFcmDelivery(
                eventId,
                userId,
                NotificationType.PAYMENT,
                ChannelType.FCM,
                staleBefore,
                claimedAt
        );
        if (updated == 0) return Optional.empty();

        return notificationDeliveryRepository.findByEventIdAndUserIdAndNotificationTypeAndChannelType(
                eventId,
                userId,
                NotificationType.PAYMENT,
                ChannelType.FCM
        );
    }
}
