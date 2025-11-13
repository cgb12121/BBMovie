package com.bbmovie.payment.dto.response;

import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SubscriptionAnalyticsResponse {
    private long activeSubscribers;
    private long autoRenewEnabled;
    private long pastEndButActive;
}


