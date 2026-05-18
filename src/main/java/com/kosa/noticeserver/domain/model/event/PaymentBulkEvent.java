package com.kosa.noticeserver.domain.model.event;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.List;

@Getter
@NoArgsConstructor
@ToString
public class PaymentBulkEvent {
    @JsonProperty("batch_id")
    private String batchId;

    @JsonProperty("event_type")
    private String eventType = "PAYMENT_BULK";

    @JsonProperty("count")
    private int count;

    @JsonProperty("events")
    private List<PaymentEvent> events;
}
