package com.kosa.noticeserver.domain.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type")
    private EventType type;

    @Column(name = "event_id")
    private String eventId;

    @Column(name = "received_at")
    private LocalDateTime receivedAt;

    @Column(name = "payload", columnDefinition = "TEXT")
    private String payload;

    public Event(EventType type, String eventId, LocalDateTime receivedAt, String payload) {
        this.type = type;
        this.eventId = eventId;
        this.receivedAt = receivedAt;
        this.payload = payload;
    }
}
