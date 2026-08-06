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
    private final NotificationDeliveryService notificationDeliveryService;

    public void notice(PaymentEvent paymentEvent, String eventId) {

        boolean isSendEnabled = notificationService.canSend(paymentEvent.getUserId(), NotificationType.PAYMENT);

        log.info("[{}] user: {}, isSendEnabled : {}", MDC.get("eventId"), paymentEvent.getUserId(), isSendEnabled);

        if (!isSendEnabled) return;

        List<String> tokens = tokenRepository.findAllTokensByUserId(paymentEvent.getUserId());

        log.info("[{}] user: {}, tokenCount : {}", MDC.get("eventId"), paymentEvent.getUserId(), tokens.size());

        if (tokens.isEmpty()) return;

        Optional<NotificationDelivery> delivery = notificationDeliveryService.claimPaymentFcmDelivery(
                eventId,
                paymentEvent.getUserId()
        );
        if (delivery.isEmpty()) return;

        List<SendNotificationCommand> commands = tokens.stream().map(token -> buildNotification(token, paymentEvent)).toList();

        SendBatchResult send;
        try {
            send = fcmsender.send(commands);
        } catch (Throwable e) {
            log.error("[Notice-Failed] FCM send error for event: {}", MDC.get("eventId"), e);
            notificationDeliveryService.markFailed(delivery.get(), e.getMessage());
            return;
        }

        if (send.successCount() > 0) {
            notificationDeliveryService.markSent(delivery.get());
        } else {
            notificationDeliveryService.markFailed(delivery.get(), firstErrorMessage(send.results()));
        }

        for (SendDetails detail : send.results()) {
            if (detail.isSuccess()) {
                log.info("[{}] user: {}, token : {}, FCM send success", MDC.get("eventId"), paymentEvent.getUserId(), detail.originalCommand().target());
            } else {
                log.error("[{}] user: {}, token : {}, FCM send failed, response : {}", MDC.get("eventId"), paymentEvent.getUserId(), detail.originalCommand().target(), detail.errorMessage());
            }
        }
    }

    public void notice(List<PaymentEvent> paymentEvents, String eventId) {

        List<String> userIds = paymentEvents.stream().map(PaymentEvent::getUserId).distinct().toList();

        Map<String, Boolean> isSendEnabledMap = notificationService.canSend(userIds, NotificationType.PAYMENT);

        userIds = paymentEvents.stream()
                .filter(
                        event -> {
                            log.info("[{}] user: {}, isSendEnabled : {}", MDC.get("eventId"), event.getUserId(), isSendEnabledMap.get(event.getUserId()));
                            return Boolean.TRUE.equals(isSendEnabledMap.get(event.getUserId()));
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

        Map<String, NotificationDelivery> claimedDeliveries = new HashMap<>();
        List<SendNotificationCommand> list = paymentEvents.stream()
                .flatMap(event -> {
                    if (tokens.isEmpty()) {
                        log.info("[{}] user: {}, tokenCount : {}", MDC.get("eventId"), event.getUserId(), tokens.size());
                        return Stream.empty();
                    }

                    List<String> userTokens = tokens.getOrDefault(event.getUserId(), Collections.emptyList());
                    log.info("[{}] user: {}, tokenCount : {}", MDC.get("eventId"), event.getUserId(), userTokens.size());
                    if (userTokens.isEmpty()) return Stream.empty();

                    Optional<NotificationDelivery> delivery = notificationDeliveryService.claimPaymentFcmDelivery(
                            eventId,
                            event.getUserId()
                    );
                    if (delivery.isEmpty()) return Stream.empty();

                    claimedDeliveries.put(event.getUserId(), delivery.get());
                    return userTokens.stream().map(token -> buildNotification(token, event));

                })
                .toList();

        if (list.isEmpty()) return;

        SendBatchResult result;
        try {
            result = fcmsender.send(list);
        } catch (Throwable e) {
            log.error("[Notice-Failed] FCM send error for event: {}", MDC.get("eventId"), e);
            claimedDeliveries.values()
                    .forEach(delivery -> notificationDeliveryService.markFailed(delivery, e.getMessage()));
            return;
        }

        markBulkDeliveryResults(result, claimedDeliveries);
        for (SendDetails detail : result.results()) {
            if (detail.isSuccess()) {
                log.info("[{}] user: {}, token : {}, FCM send success", MDC.get("eventId"), detail.originalCommand().data().getOrDefault("userId", ""), detail.originalCommand().target());
            } else {
                log.error("[{}] user: {}, token : {}, FCM send failed", MDC.get("eventId"), detail.originalCommand().data().getOrDefault("userId", ""), detail.originalCommand().target());
            }
        }

    }

    private void markBulkDeliveryResults(
            SendBatchResult result,
            Map<String, NotificationDelivery> claimedDeliveries
    ) {
        Map<String, List<SendDetails>> detailsByUserId = result.results().stream()
                .collect(Collectors.groupingBy(detail -> detail.originalCommand().data().getOrDefault("userId", "")));

        for (Map.Entry<String, NotificationDelivery> entry : claimedDeliveries.entrySet()) {
            List<SendDetails> details = detailsByUserId.getOrDefault(entry.getKey(), Collections.emptyList());
            boolean hasSuccess = details.stream().anyMatch(SendDetails::isSuccess);
            if (hasSuccess) {
                notificationDeliveryService.markSent(entry.getValue());
            } else {
                notificationDeliveryService.markFailed(entry.getValue(), firstErrorMessage(details));
            }
        }
    }

    private String firstErrorMessage(List<SendDetails> details) {
        return details.stream()
                .map(SendDetails::errorMessage)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse("FCM send failed");
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
