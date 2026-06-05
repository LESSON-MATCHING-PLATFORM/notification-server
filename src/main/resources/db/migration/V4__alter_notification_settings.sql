-- notification_settings 테이블에 userId, notification_type 유니크 제약조건 추가

ALTER TABLE notification_setting_entity
    ADD CONSTRAINT uk_notification_setting_user_type
        UNIQUE (user_id, notification_type);
