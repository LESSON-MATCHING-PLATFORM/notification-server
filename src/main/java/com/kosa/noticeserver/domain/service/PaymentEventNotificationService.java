package com.kosa.noticeserver.domain.service;

import com.kosa.noticeserver.domain.model.NotificationType;
import com.kosa.noticeserver.domain.model.SendBatchResult;
import com.kosa.noticeserver.domain.model.TokenEntity;
import com.kosa.noticeserver.domain.model.event.PaymentEvent;
import com.kosa.noticeserver.domain.model.SendNotificationCommand;
import com.kosa.noticeserver.infrastructure.repository.TokenRepository;
import com.kosa.noticeserver.infrastructure.sender.fcm.FCMSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentEventNotificationService {

    private final TokenRepository tokenRepository;
    private final FCMSender fcmsender;
    private final NotificationService notificationService;

    public void notice(PaymentEvent paymentEvent) {

        boolean isSendEnabled = notificationService.canSend(paymentEvent.getUserId(), NotificationType.PAYMENT);

        if (!isSendEnabled) {
            log.info("[Notice-Skip] Disabled user: {}, event: {}", paymentEvent.getUserId(), paymentEvent.getId());
            return;
        }

        List<String> tokens = tokenRepository.findAllTokensByUserId(paymentEvent.getUserId());

        if (tokens.isEmpty()) {
            log.info("[Notice-Skip] No tokens for user: {}, event: {}", paymentEvent.getUserId(), paymentEvent.getId());
            return;
        }

        List<SendNotificationCommand> commands = tokens.stream().map(token -> buildNotification(token, paymentEvent)).toList();

        try {
            fcmsender.send(commands);
            log.info("[Notice-Success] Sent FCM for event: {}", paymentEvent.getId());
        } catch (Throwable e) {
            log.error("[Notice-Failed] FCM send error for event: {}", paymentEvent.getId(), e);
            e.printStackTrace();
        }
    }

    public void notice(List<PaymentEvent> paymentEvents) {

        List<String> userIds = paymentEvents.stream().map(PaymentEvent::getUserId).distinct().toList();

        Map<String, Boolean> isSendEnabledMap = notificationService.canSend(userIds, NotificationType.PAYMENT);

        userIds = paymentEvents.stream()
                .filter(
                        event -> {
                            if (!isSendEnabledMap.get(event.getUserId())) {
                                log.info("[Notice-Skip] Disabled user: {}, event: {}", event.getUserId(), event.getId());
                                return false;
                            }
                            return true;
                        }
                )
                .map(PaymentEvent::getUserId)
                .distinct().toList();

        List<TokenEntity> allByUserIdIn = tokenRepository.findAllByUserIdIn(userIds);
        Map<String, List<String>> tokens = allByUserIdIn.stream()
                .collect(
                        Collectors.groupingBy(
                                TokenEntity::getUserId,
                                Collectors.mapping(TokenEntity::getToken, Collectors.toList())
                        )
                );

        List<SendNotificationCommand> list = paymentEvents.stream()
                .flatMap(event -> {
                    if (!isSendEnabledMap.get(event.getUserId())) {
                        log.info("[Notice-Skip] Disabled user: {}, event: {}", event.getUserId(), event.getId());
                        return Stream.empty();
                    }

                    if (tokens.isEmpty()) {
                        log.info("[Notice-Skip] No tokens for user: {}, event: {}", event.getUserId(), event.getId());
                        return Stream.empty();
                    }

                    return tokens.get(event.getUserId()).stream().map(token -> buildNotification(token, event));

                })
                .toList();

        try {
            SendBatchResult result = fcmsender.send(list);
            result.results().stream().forEach(notification -> {
                if (notification.isSuccess()) {
                    log.info("[Notice-Success] Sent FCM for event: {}", notification.messageId());
                } else {
                    log.error("[Notice-Failed] FCM send error for event: {}", notification.messageId());
                }
            });
        } catch (Throwable e) {
            e.printStackTrace();
        }

    }

    private SendNotificationCommand buildNotification(String token, PaymentEvent event) {
        String title = "결제 완료 안내";
        String body = String.format("%s님, %s원 결제가 정상 처리되었습니다.",
                event.getUserName(), event.getAmount());

        Map<String, String> data = new HashMap<>();
        data.put("orderId", event.getOrderId());
        data.put("paymentTime", event.getTimestamp());

        return new SendNotificationCommand(
                token,
                title,
                body,
                data,
                null
        );
    }
}
