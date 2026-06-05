package com.kosa.noticeserver.presentaion;

import com.kosa.noticeserver.domain.model.NotificationSettingEntity;
import com.kosa.noticeserver.domain.model.TokenEntity;
import com.kosa.noticeserver.domain.service.NotificationService;
import com.kosa.noticeserver.presentaion.dto.SaveTokenRequest;
import com.kosa.noticeserver.presentaion.dto.UpdateNotificationSettingRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/notification")
public class NotificationController {

    private final NotificationService notificationService;

    @PostMapping("/token")
    public void saveToken(
            @Valid @RequestBody SaveTokenRequest request
    ) {
        notificationService.saveToken(new TokenEntity(
                request.token(), request.userId()
        ));
    }

    @PostMapping("/setting")
    public void updateNotificationSetting(
            @Valid @RequestBody UpdateNotificationSettingRequest request
    ) {
        notificationService.updateNotificationSetting(
                new NotificationSettingEntity(
                        request.userId(), request.type(), request.enabled()
                )
        );
    }

}
