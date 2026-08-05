package com.kosa.noticeserver.infrastructure.repository;

import com.kosa.noticeserver.domain.model.ChannelType;
import com.kosa.noticeserver.domain.model.NotificationDelivery;
import com.kosa.noticeserver.domain.model.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface NotificationDeliveryRepository extends JpaRepository<NotificationDelivery, Long> {

    Optional<NotificationDelivery> findByEventIdAndUserIdAndNotificationTypeAndChannelType(
            String eventId,
            String userId,
            NotificationType notificationType,
            ChannelType channelType
    );
}
