package com.bbmovie.payment.dto.response;

import com.bbmovie.payment.entity.enums.PaymentProvider;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserSubscriptionResponse {
    private UUID id;
    private UUID planId;
    private String userId;
    private boolean active;
    private boolean autoRenew;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private LocalDateTime lastPaymentDate;
    private LocalDateTime nextPaymentDate;
    private PaymentProvider paymentProvider;
    private String paymentMethod;
}


