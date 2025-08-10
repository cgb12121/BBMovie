package com.bbmovie.payment.entity;

import com.bbmovie.payment.entity.base.BaseEntity;
import com.bbmovie.payment.entity.enums.PaymentProvider;
import com.bbmovie.payment.entity.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Builder
@Table(name = "payment_transactions")
@NoArgsConstructor
@AllArgsConstructor
public class PaymentTransaction extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private String userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_id")
    private UserSubscription subscription;

    @Column(name = "amount", nullable = false)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false)
    private String currency;

    @Column(name = "payment_provider", nullable = false)
    @Enumerated(EnumType.STRING)
    private PaymentProvider paymentProvider;

    @Column(name = "payment_method", nullable = false)
    private String paymentMethod;

    @Column(name = "payment_gateway_id", columnDefinition = "TEXT")
    private String paymentGatewayId;

    @Column(name = "payment_gateway_order_id", columnDefinition = "TEXT")
    private String paymentGatewayOrderId;

    @Column(name = "provider_status", nullable = false)
    private String providerStatus;

    @Column(name = "transaction_date", nullable = false)
    private LocalDateTime transactionDate;

    @Column(name = "cancel_date")
    private LocalDateTime cancelDate;

    @Column(name = "refund_date")
    private LocalDateTime refundDate;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private PaymentStatus status;

    @Column(name = "error_code")
    private String errorCode;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "ipn_url", columnDefinition = "TEXT")
    private String ipnUrl;

    @Column(name = "return_url", columnDefinition = "TEXT")
    private String returnUrl;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "payment_details", columnDefinition = "TEXT")
    private String paymentDetails;
}
