package com.kosa.noticeserver.domain.service;

import com.kosa.noticeserver.domain.model.NotificationDelivery;
import com.kosa.noticeserver.infrastructure.repository.NotificationDeliveryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class NotificationDeliveryClaimer {

    private final NotificationDeliveryRepository notificationDeliveryRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public NotificationDelivery claim(NotificationDelivery delivery) {
        return notificationDeliveryRepository.saveAndFlush(delivery);
    }
}
