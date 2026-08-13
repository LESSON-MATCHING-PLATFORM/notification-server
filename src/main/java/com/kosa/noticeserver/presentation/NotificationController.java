package com.kosa.noticeserver.presentation;

import com.kosa.noticeserver.domain.model.NotificationSettingEntity;
import com.kosa.noticeserver.domain.model.TokenEntity;
import com.kosa.noticeserver.domain.service.NotificationService;
import com.kosa.noticeserver.presentation.dto.SaveTokenRequest;
import com.kosa.noticeserver.presentation.dto.UpdateNotificationSettingRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/notification")
public class NotificationController {

    private final NotificationService notificationService;

    @PostMapping("/token")
    public void saveToken(
            @RequestHeader("X-User-Id") String userId,
            @Valid @RequestBody SaveTokenRequest request
    ) {
        notificationService.saveToken(new TokenEntity(
                request.token(), userId
        ));
    }

    @PostMapping("/setting")
    public void updateNotificationSetting(
            @RequestHeader("X-User-Id") String userId,
            @Valid @RequestBody UpdateNotificationSettingRequest request
    ) {
        notificationService.updateNotificationSetting(
                new NotificationSettingEntity(
                        userId, request.type(), request.enabled()
                )
        );
    }

}
