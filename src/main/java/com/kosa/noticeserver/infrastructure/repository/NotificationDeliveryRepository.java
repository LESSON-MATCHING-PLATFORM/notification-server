package com.kosa.noticeserver.infrastructure.repository;

import com.kosa.noticeserver.domain.model.ChannelType;
import com.kosa.noticeserver.domain.model.NotificationDelivery;
import com.kosa.noticeserver.domain.model.NotificationDeliveryStatus;
import com.kosa.noticeserver.domain.model.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.Optional;

public interface NotificationDeliveryRepository extends JpaRepository<NotificationDelivery, Long> {

    Optional<NotificationDelivery> findByEventIdAndUserIdAndNotificationTypeAndChannelType(
            String eventId,
            String userId,
            NotificationType notificationType,
            ChannelType channelType
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update NotificationDelivery delivery
            set delivery.status = com.kosa.noticeserver.domain.model.NotificationDeliveryStatus.CLAIMED,
                delivery.claimedAt = :claimedAt,
                delivery.completedAt = null,
                delivery.lastError = null
            where delivery.eventId = :eventId
                and delivery.userId = :userId
                and delivery.notificationType = :notificationType
                and delivery.channelType = :channelType
                and (
                    delivery.status = com.kosa.noticeserver.domain.model.NotificationDeliveryStatus.FAILED
                    or (
                        delivery.status = com.kosa.noticeserver.domain.model.NotificationDeliveryStatus.CLAIMED
                        and delivery.claimedAt < :staleBefore
                    )
                )
            """)
    int reclaimPaymentFcmDelivery(
            String eventId,
            String userId,
            NotificationType notificationType,
            ChannelType channelType,
            LocalDateTime staleBefore,
            LocalDateTime claimedAt
    );

    long countByStatusAndClaimedAtBefore(NotificationDeliveryStatus status, LocalDateTime claimedAt);
}
