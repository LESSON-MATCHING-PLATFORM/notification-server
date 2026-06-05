package com.kosa.noticeserver.domain.model;

public enum NotificationType {
    PAYMENT("결제 알림"),
    ;

    private final String name;

    NotificationType(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
