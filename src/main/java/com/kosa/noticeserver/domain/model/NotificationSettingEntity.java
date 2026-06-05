package com.kosa.noticeserver.domain.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NotificationSettingEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private String userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type")
    private NotificationType type;

    @Setter
    @Column(name = "is_enabled")
    private boolean isEnabled;

    public NotificationSettingEntity(
            String userId, NotificationType type, boolean isEnabled
    ) {
        this.userId = userId;
        this.type = type;
        this.isEnabled = isEnabled;
    }
}
