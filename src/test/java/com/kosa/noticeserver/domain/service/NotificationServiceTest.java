package com.kosa.noticeserver.domain.service;

import com.kosa.noticeserver.domain.model.NotificationSettingEntity;
import com.kosa.noticeserver.domain.model.NotificationType;
import com.kosa.noticeserver.infrastructure.repository.NotificationSettingRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Testcontainers
@Transactional
class NotificationServiceTest {

    @Autowired
    private NotificationSettingRepository notificationSettingRepository;
    @Autowired
    private NotificationService notificationService;
    @Autowired
    private EntityManager em;

    @Container
    @ServiceConnection
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.4");

    @Test
    public void 알림_설정을_저장합니다() {
        // given, when
        notificationService.updateNotificationSetting(
                new NotificationSettingEntity(
                        "user_id",
                        NotificationType.PAYMENT,
                        true
                )
        );
        em.flush();
        em.clear();

        // then
        NotificationSettingEntity setting = notificationSettingRepository
                .findByUserIdAndType("user_id", NotificationType.PAYMENT)
                .orElseThrow(RuntimeException::new);

        assertNotNull(setting);
        assertEquals(NotificationType.PAYMENT, setting.getType());
        assertTrue(setting.isEnabled());
    }

    @Test
    public void 설정이_이미_존재할_경우_업데이트한다() {
        // given
        notificationService.updateNotificationSetting(
                new NotificationSettingEntity(
                        "user_id",
                        NotificationType.PAYMENT,
                        true
                )
        );
        em.flush();
        em.clear();

        // when
        notificationService.updateNotificationSetting(
                new NotificationSettingEntity(
                        "user_id",
                        NotificationType.PAYMENT,
                        false
                )
        );
        em.flush();
        em.clear();

        // then
        NotificationSettingEntity setting = notificationSettingRepository
                .findByUserIdAndType("user_id", NotificationType.PAYMENT)
                .orElseThrow(RuntimeException::new);

        assertNotNull(setting);
        assertEquals(NotificationType.PAYMENT, setting.getType());
        assertFalse(setting.isEnabled());
    }


}