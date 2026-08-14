package com.kosa.noticeserver.domain.service;

import com.kosa.noticeserver.domain.model.NotificationSettingEntity;
import com.kosa.noticeserver.domain.model.NotificationType;
import com.kosa.noticeserver.domain.model.TokenEntity;
import com.kosa.noticeserver.infrastructure.repository.NotificationSettingRepository;
import com.kosa.noticeserver.infrastructure.repository.TokenRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
        "spring.kafka.listener.auto-startup=false"
})
@Testcontainers
@Transactional
class NotificationServiceTest {

    @Autowired
    private NotificationSettingRepository notificationSettingRepository;
    @Autowired
    private TokenRepository tokenRepository;
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

    @Test
    public void 동일한_FCM_토큰이_다시_저장되면_현재_사용자로_갱신한다() {
        // given
        notificationService.saveToken(new TokenEntity("fcm-token-001", "user-001"));
        em.flush();
        em.clear();

        // when
        notificationService.saveToken(new TokenEntity("fcm-token-001", "user-002"));
        em.flush();
        em.clear();

        // then
        TokenEntity token = tokenRepository.findByToken("fcm-token-001")
                .orElseThrow(RuntimeException::new);

        assertEquals(1, tokenRepository.count());
        assertEquals("user-002", token.getUserId());
        assertNotNull(token.getCreatedAt());
        assertNotNull(token.getUpdatedAt());
    }

    @Test
    public void 토큰을_저장하면_결제_알림_기본_설정을_생성한다() {
        // when
        notificationService.saveToken(new TokenEntity("fcm-token-002", "user-003"));
        em.flush();
        em.clear();

        // then
        NotificationSettingEntity setting = notificationSettingRepository
                .findByUserIdAndType("user-003", NotificationType.PAYMENT)
                .orElseThrow(RuntimeException::new);

        assertTrue(setting.isEnabled());
    }

    @Test
    public void 기존_알림_설정이_있으면_토큰_저장시_설정을_덮어쓰지_않는다() {
        // given
        notificationService.updateNotificationSetting(
                new NotificationSettingEntity("user-004", NotificationType.PAYMENT, false)
        );
        em.flush();
        em.clear();

        // when
        notificationService.saveToken(new TokenEntity("fcm-token-003", "user-004"));
        em.flush();
        em.clear();

        // then
        NotificationSettingEntity setting = notificationSettingRepository
                .findByUserIdAndType("user-004", NotificationType.PAYMENT)
                .orElseThrow(RuntimeException::new);

        assertFalse(setting.isEnabled());
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void 외부_트랜잭션이_없어도_토큰_목록을_삭제한다() {
        // given
        tokenRepository.saveAll(List.of(
                new TokenEntity("invalid-token-001", "user-005"),
                new TokenEntity("valid-token-001", "user-005")
        ));

        try {
            // when, then
            assertDoesNotThrow(() -> tokenRepository.deleteByTokenIn(List.of("invalid-token-001")));
            assertTrue(tokenRepository.findByToken("invalid-token-001").isEmpty());
            assertTrue(tokenRepository.findByToken("valid-token-001").isPresent());
        } finally {
            tokenRepository.deleteByTokenIn(List.of("invalid-token-001", "valid-token-001"));
        }
    }

}
