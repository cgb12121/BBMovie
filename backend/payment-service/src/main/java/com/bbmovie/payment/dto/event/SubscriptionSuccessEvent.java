package com.bbmovie.payment.dto.event;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SubscriptionSuccessEvent {
    private String userId;
    private String transactionId;
    private String amount;
}
