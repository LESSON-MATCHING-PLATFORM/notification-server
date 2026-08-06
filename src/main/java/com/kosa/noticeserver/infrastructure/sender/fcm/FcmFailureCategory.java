package com.kosa.noticeserver.infrastructure.sender.fcm;

public enum FcmFailureCategory {
    TOKEN_INVALID,
    RETRYABLE,
    CONFIGURATION,
    AMBIGUOUS,
    UNKNOWN
}
