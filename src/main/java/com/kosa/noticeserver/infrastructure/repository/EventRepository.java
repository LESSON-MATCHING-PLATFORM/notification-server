package com.kosa.noticeserver.infrastructure.repository;

import com.kosa.noticeserver.domain.model.Event;
import com.kosa.noticeserver.domain.model.EventType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventRepository extends JpaRepository<Event, Long> {
    Page<Event> findAllByTypeOrderByReceivedAtDesc(EventType eventType, PageRequest pageRequest);
}
