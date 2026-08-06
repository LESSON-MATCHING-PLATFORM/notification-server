package com.kosa.noticeserver.infrastructure.sender.fcm;

import com.kosa.noticeserver.domain.model.SendDetails;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class FcmFailureClassifier {

    private static final Set<String> TOKEN_INVALID_CODES = Set.of(
            "UNREGISTERED",
            "SENDER_ID_MISMATCH"
    );
    private static final Set<String> RETRYABLE_CODES = Set.of(
            "UNAVAILABLE",
            "INTERNAL",
            "QUOTA_EXCEEDED"
    );
    private static final Set<String> CONFIGURATION_CODES = Set.of(
            "THIRD_PARTY_AUTH_ERROR"
    );

    public FcmFailureCategory classify(SendDetails details) {
        if (details == null || details.isSuccess()) {
            return FcmFailureCategory.UNKNOWN;
        }

        String errorCode = details.errorCode();
        if (errorCode == null || errorCode.isBlank()) {
            return FcmFailureCategory.UNKNOWN;
        }

        if (TOKEN_INVALID_CODES.contains(errorCode)) {
            return FcmFailureCategory.TOKEN_INVALID;
        }

        if (RETRYABLE_CODES.contains(errorCode)) {
            return FcmFailureCategory.RETRYABLE;
        }

        if (CONFIGURATION_CODES.contains(errorCode)) {
            return FcmFailureCategory.CONFIGURATION;
        }

        return FcmFailureCategory.UNKNOWN;
    }

    public FcmFailureCategory classify(Throwable throwable) {
        return FcmFailureCategory.AMBIGUOUS;
    }

    public boolean isInvalidToken(SendDetails details) {
        return classify(details) == FcmFailureCategory.TOKEN_INVALID;
    }
}
