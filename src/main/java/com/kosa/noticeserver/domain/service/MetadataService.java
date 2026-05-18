package com.kosa.noticeserver.domain.service;

import com.kosa.noticeserver.domain.model.Event;
import com.kosa.noticeserver.domain.model.EventType;
import com.kosa.noticeserver.infrastructure.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MetadataService {

    private final EventRepository eventRepository;

    public Page<Event> getConsumedEvent(EventType eventType, int page, int size) {
        return eventRepository.findAllByTypeOrderByReceivedAtDesc(eventType, PageRequest.of(page, size));
    }
}
