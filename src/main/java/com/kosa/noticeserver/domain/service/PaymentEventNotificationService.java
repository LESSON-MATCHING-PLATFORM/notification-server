package com.kosa.noticeserver.domain.service;

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

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentEventNotificationService {

    private final TokenRepository tokenRepository;
    private final FCMSender fcmsender;

    public void notice(PaymentEvent paymentEvent) {
        List<String> tokens = tokenRepository.findAllTokensByUserId(paymentEvent.getUserId());

        if (tokens.isEmpty()) {
            tokens = List.of("d6UgfPSy4wc221Wsv3wQnQ:APA91bHlmQgskhLyw7MovsRWYJovMa902JXhtzlyBPUE8y1VqMgYI6Q4Mdd8ducJv9qEdZp8ProeF9TVXOKD6xhpS2zpx9q3F9N_pbLYrWJa6PNpjQHoCSA");
        }

        List<SendNotificationCommand> commands = tokens.stream().map(token -> buildNotification(token, paymentEvent)).toList();

        try {
            fcmsender.send(commands);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    public void notice(List<PaymentEvent> paymentEvents) {

        List<String> userIds = paymentEvents.stream().map(PaymentEvent::getUserId).distinct().toList();

        List<TokenEntity> allByUserIdIn = tokenRepository.findAllByUserIdIn(userIds);
        Map<String, List<String>> tokens = allByUserIdIn.stream().collect(
                Collectors.groupingBy(TokenEntity::getUserId, Collectors.mapping(TokenEntity::getToken, Collectors.toList())));

        List<SendNotificationCommand> list = paymentEvents.stream()
                .flatMap(event ->
                        tokens.getOrDefault(event.getUserId(), List.of("d6UgfPSy4wc221Wsv3wQnQ:APA91bHlmQgskhLyw7MovsRWYJovMa902JXhtzlyBPUE8y1VqMgYI6Q4Mdd8ducJv9qEdZp8ProeF9TVXOKD6xhpS2zpx9q3F9N_pbLYrWJa6PNpjQHoCSA"))
                                .stream().map(token -> buildNotification(token, event)))
                .toList();

        try {
            fcmsender.send(list);
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
