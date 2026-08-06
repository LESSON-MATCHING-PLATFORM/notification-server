CREATE TABLE notification_delivery (
    id BIGINT NOT NULL AUTO_INCREMENT,
    event_id VARCHAR(191) NOT NULL,
    user_id VARCHAR(191) NOT NULL,
    notification_type VARCHAR(50) NOT NULL,
    channel_type VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL,
    claimed_at DATETIME(6) NOT NULL,
    completed_at DATETIME(6),
    last_error TEXT,
    PRIMARY KEY (id),
    CONSTRAINT uk_notification_delivery_event_recipient
        UNIQUE (event_id, user_id, notification_type, channel_type)
) ENGINE=InnoDB;
