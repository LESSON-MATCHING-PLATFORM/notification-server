package com.kosa.noticeserver.domain.service;

import com.kosa.noticeserver.domain.model.*;
import com.kosa.noticeserver.domain.model.event.PaymentEvent;
import com.kosa.noticeserver.infrastructure.repository.TokenRepository;
import com.kosa.noticeserver.infrastructure.sender.fcm.FCMSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jboss.logging.MDC;
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

        log.info("[{}}] user: {}, isSendEnabled : {}", MDC.get("eventId"), paymentEvent.getUserId(), isSendEnabled);

        if (!isSendEnabled) return;

        List<String> tokens = tokenRepository.findAllTokensByUserId(paymentEvent.getUserId());

        log.info("[{}}] user: {}, tokenCount : {}", MDC.get("eventId"), paymentEvent.getUserId(), tokens.size());

        if (tokens.isEmpty()) return;

        List<SendNotificationCommand> commands = tokens.stream().map(token -> buildNotification(token, paymentEvent)).toList();

        try {
            SendBatchResult send = fcmsender.send(commands);

            for (SendDetails detail : send.results()) {
                if (detail.isSuccess()) {
                    log.info("[{}}] user: {}, token : {}, FCM send success", MDC.get("eventId"), paymentEvent.getUserId(), detail.originalCommand().target());
                } else {
                    log.error("[{}}] user: {}, token : {}, FCM send failed, respoonse : {}", MDC.get("eventId"), paymentEvent.getUserId(), detail.originalCommand().target(), detail.errorMessage());
                }
            }
        } catch (Throwable e) {
            log.error("[Notice-Failed] FCM send error for event: {}", MDC.get("eventId"), e);
        }
    }

    public void notice(List<PaymentEvent> paymentEvents) {

        List<String> userIds = paymentEvents.stream().map(PaymentEvent::getUserId).distinct().toList();

        Map<String, Boolean> isSendEnabledMap = notificationService.canSend(userIds, NotificationType.PAYMENT);

        userIds = paymentEvents.stream()
                .filter(
                        event -> {
                            log.info("[{}}] user: {}, isSendEnabled : {}", MDC.get("eventId"), event.getUserId(), isSendEnabledMap.get(event.getUserId()));
                            return isSendEnabledMap.get(event.getUserId());
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
                    if (tokens.isEmpty()) {
                        log.info("[{}}] user: {}, tokenCount : {}", MDC.get("eventId"), event.getUserId(), tokens.size());
                        return Stream.empty();
                    }

                    return tokens.get(event.getUserId()).stream().map(token -> buildNotification(token, event));

                })
                .toList();

        try {
            SendBatchResult result = fcmsender.send(list);
            for (SendDetails detail : result.results()) {
                if (detail.isSuccess()) {
                    log.info("[{}}] user: {}, token : {}, FCM send success", MDC.get("eventId"), detail.originalCommand().data().getOrDefault("userId", ""), detail.originalCommand().target());
                } else {
                    log.error("[{}}] user: {}, token : {}, FCM send failed", MDC.get("eventId"), detail.originalCommand().data().getOrDefault("userId", ""), detail.originalCommand().target());
                }
            }
        } catch (Throwable e) {
            log.error("[Notice-Failed] FCM send error for event: {}", MDC.get("eventId"), e);
        }

    }

    private SendNotificationCommand buildNotification(String token, PaymentEvent event) {
        String title = "결제 완료 안내";
        String body = String.format("%s님, %s원 결제가 정상 처리되었습니다.",
                event.getUserName(), event.getAmount());

        Map<String, String> data = new HashMap<>();
        data.put("orderId", event.getOrderId());
        data.put("paymentTime", event.getTimestamp());
        data.put("userId", event.getUserId());

        return new SendNotificationCommand(
                token,
                title,
                body,
                data,
                null
        );
    }
}
