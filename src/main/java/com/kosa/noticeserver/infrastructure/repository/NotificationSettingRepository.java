package com.kosa.noticeserver.infrastructure.repository;

import com.kosa.noticeserver.domain.model.NotificationSettingEntity;
import com.kosa.noticeserver.domain.model.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationSettingRepository extends JpaRepository<NotificationSettingEntity, Long> {
    Optional<NotificationSettingEntity> findByUserIdAndType(String userId, NotificationType notificationType);

    List<NotificationSettingEntity> findAllByUserIdInAndType(List<String> userIds, NotificationType type);
}
