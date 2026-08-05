package com.kosa.noticeserver.domain.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "notification_delivery",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_notification_delivery_event_recipient",
                        columnNames = {"event_id", "user_id", "notification_type", "channel_type"}
                )
        }
)
public class NotificationDelivery {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false)
    private String eventId;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", nullable = false)
    private NotificationType notificationType;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel_type", nullable = false)
    private ChannelType channelType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private NotificationDeliveryStatus status;

    @Column(name = "claimed_at", nullable = false)
    private LocalDateTime claimedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;

    public NotificationDelivery(
            String eventId,
            String userId,
            NotificationType notificationType,
            ChannelType channelType,
            LocalDateTime claimedAt
    ) {
        this.eventId = eventId;
        this.userId = userId;
        this.notificationType = notificationType;
        this.channelType = channelType;
        this.status = NotificationDeliveryStatus.CLAIMED;
        this.claimedAt = claimedAt;
    }

    public void markSent(LocalDateTime completedAt) {
        this.status = NotificationDeliveryStatus.SENT;
        this.completedAt = completedAt;
        this.lastError = null;
    }

    public void markFailed(LocalDateTime completedAt, String errorMessage) {
        this.status = NotificationDeliveryStatus.FAILED;
        this.completedAt = completedAt;
        this.lastError = errorMessage;
    }
}
