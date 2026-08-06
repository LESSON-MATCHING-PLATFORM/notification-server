package com.kosa.noticeserver.infrastructure.sender.fcm;

import com.google.firebase.ErrorCode;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.MessagingErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FCMSenderErrorCodeTest {

    @Test
    @DisplayName("FCM 전용 오류 코드가 있으면 공통 오류 코드보다 우선 저장한다")
    void resolveErrorCode_whenMessagingErrorCodeExists_returnsMessagingErrorCode() {
        FirebaseMessagingException exception = mock(FirebaseMessagingException.class);
        when(exception.getMessagingErrorCode()).thenReturn(MessagingErrorCode.UNREGISTERED);
        when(exception.getErrorCode()).thenReturn(ErrorCode.NOT_FOUND);

        String errorCode = FCMSender.resolveErrorCode(exception);

        assertThat(errorCode).isEqualTo("UNREGISTERED");
    }

    @Test
    @DisplayName("FCM 전용 오류 코드가 없으면 공통 오류 코드로 대체한다")
    void resolveErrorCode_whenMessagingErrorCodeIsMissing_returnsBaseErrorCode() {
        FirebaseMessagingException exception = mock(FirebaseMessagingException.class);
        when(exception.getMessagingErrorCode()).thenReturn(null);
        when(exception.getErrorCode()).thenReturn(ErrorCode.UNAVAILABLE);

        String errorCode = FCMSender.resolveErrorCode(exception);

        assertThat(errorCode).isEqualTo("UNAVAILABLE");
    }

    @Test
    @DisplayName("예외가 없으면 오류 코드를 저장하지 않는다")
    void resolveErrorCode_whenExceptionIsMissing_returnsNull() {
        assertThat(FCMSender.resolveErrorCode(null)).isNull();
    }
}
