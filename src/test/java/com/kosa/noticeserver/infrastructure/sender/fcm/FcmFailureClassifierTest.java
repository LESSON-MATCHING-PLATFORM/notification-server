package com.kosa.noticeserver.infrastructure.sender.fcm;

import com.kosa.noticeserver.domain.model.SendDetails;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FcmFailureClassifierTest {

    private final FcmFailureClassifier classifier = new FcmFailureClassifier();

    @Test
    @DisplayName("UNREGISTERED와 SENDER_ID_MISMATCH는 토큰 정리 대상으로 분류한다")
    void classify_whenTokenIsInvalid_returnsTokenInvalid() {
        assertThat(classifier.classify(details("UNREGISTERED"))).isEqualTo(FcmFailureCategory.TOKEN_INVALID);
        assertThat(classifier.classify(details("SENDER_ID_MISMATCH"))).isEqualTo(FcmFailureCategory.TOKEN_INVALID);
    }

    @Test
    @DisplayName("일시적 FCM 오류는 retryable로 분류한다")
    void classify_whenFailureIsTemporary_returnsRetryable() {
        assertThat(classifier.classify(details("UNAVAILABLE"))).isEqualTo(FcmFailureCategory.RETRYABLE);
        assertThat(classifier.classify(details("INTERNAL"))).isEqualTo(FcmFailureCategory.RETRYABLE);
        assertThat(classifier.classify(details("QUOTA_EXCEEDED"))).isEqualTo(FcmFailureCategory.RETRYABLE);
    }

    @Test
    @DisplayName("인증 설정 오류는 configuration으로 분류한다")
    void classify_whenFailureIsConfiguration_returnsConfiguration() {
        assertThat(classifier.classify(details("THIRD_PARTY_AUTH_ERROR"))).isEqualTo(FcmFailureCategory.CONFIGURATION);
    }

    @Test
    @DisplayName("모호하거나 성공한 응답은 토큰 삭제 대상으로 분류하지 않는다")
    void classify_whenFailureIsAmbiguous_returnsUnknown() {
        assertThat(classifier.classify(details("INVALID_ARGUMENT"))).isEqualTo(FcmFailureCategory.UNKNOWN);
        assertThat(classifier.classify(details(null))).isEqualTo(FcmFailureCategory.UNKNOWN);
        assertThat(classifier.classify(new SendDetails(true, "message-001", null, null, null)))
                .isEqualTo(FcmFailureCategory.UNKNOWN);
    }

    @Test
    @DisplayName("요청 단위 예외는 개별 토큰 상태를 알 수 없으므로 ambiguous로 분류한다")
    void classify_whenRequestThrows_returnsAmbiguous() {
        assertThat(classifier.classify(new IllegalStateException("fcm down")))
                .isEqualTo(FcmFailureCategory.AMBIGUOUS);
    }

    private SendDetails details(String errorCode) {
        return new SendDetails(false, null, "failed", errorCode, null);
    }
}
