package com.kosa.noticeserver.domain.model;

import java.util.List;

public record SendBatchResult(
        List<SendDetails> results,
        int successCount,
        int failedCount
) {
}
