package com.kosa.noticeserver.infrastructure.sender;

import com.kosa.noticeserver.domain.model.ChannelType;
import com.kosa.noticeserver.domain.model.EventType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface NotificationTrace {
    EventType type();
    ChannelType channel();
}
