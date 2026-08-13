package com.kosa.noticeserver.domain.service;

import com.kosa.noticeserver.domain.model.NotificationSettingEntity;
import com.kosa.noticeserver.domain.model.NotificationType;
import com.kosa.noticeserver.domain.model.TokenEntity;
import com.kosa.noticeserver.infrastructure.repository.NotificationSettingRepository;
import com.kosa.noticeserver.infrastructure.repository.TokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final TokenRepository tokenRepository;
    private final NotificationSettingRepository notificationSettingRepository;

    @Transactional
    public void saveToken(TokenEntity tokenEntity) {
        log.info("FCM token registration requested. userId={}", tokenEntity.getUserId());
        tokenRepository.upsertByToken(tokenEntity.getToken(), tokenEntity.getUserId());
    }

    @Transactional
    public void updateNotificationSetting(NotificationSettingEntity setting) {

        notificationSettingRepository.findByUserIdAndType(setting.getUserId(), setting.getType())
                .ifPresentOrElse(
                        entity -> {
                            if (entity.isEnabled() != setting.isEnabled()) {
                                entity.setEnabled(!entity.isEnabled());
                            }
                        },
                        () -> notificationSettingRepository.save(setting)
                );
    }

    public boolean canSend(String userId, NotificationType type) {
        return notificationSettingRepository.findByUserIdAndType(userId, type)
                .map(NotificationSettingEntity::isEnabled).orElse(false);
    }

    public Map<String, Boolean> canSend(List<String> userIds, NotificationType type) {
        List<NotificationSettingEntity> settings = notificationSettingRepository.findAllByUserIdInAndType(userIds, type);

        return settings.stream().collect(Collectors.toMap(NotificationSettingEntity::getUserId, NotificationSettingEntity::isEnabled));
    }
}
