package com.kosa.noticeserver.domain.service;

import com.kosa.noticeserver.domain.model.Event;
import com.kosa.noticeserver.domain.model.EventType;
import com.kosa.noticeserver.infrastructure.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;


@Service
@RequiredArgsConstructor
public class MetadataService {

    private final EventRepository eventRepository;

    public Page<Event> getConsumedEvent(EventType eventType, int page, int size) {
        return eventRepository.findAllByTypeOrderByReceivedAtDesc(eventType, PageRequest.of(page, size));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Event recordConsumedEvent(EventType eventType, String eventId, String payload) {
        return eventRepository.save(new Event(eventType, eventId, LocalDateTime.now(), payload));
    }
}
