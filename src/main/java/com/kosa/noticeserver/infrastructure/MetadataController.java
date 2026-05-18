package com.kosa.noticeserver.infrastructure;

import com.kosa.noticeserver.domain.model.Event;
import com.kosa.noticeserver.domain.model.EventType;
import com.kosa.noticeserver.domain.service.MetadataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/metadata")
@Slf4j
public class MetadataController {

    private final MetadataService metadataService;

    @RequestMapping("/event/consume")
    public Page<Event> getConsumeEvent(
            @RequestParam EventType eventType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        log.info("request metadata for consume event:");
        return metadataService.getConsumedEvent(eventType, page, size);
    }

}
