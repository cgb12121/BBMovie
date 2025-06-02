package com.example.bbmovie.entity;

import com.example.bbmovie.entity.base.BaseEntity;
import com.example.bbmovie.entity.enumerate.PaymentProvider;
import com.example.bbmovie.entity.enumerate.PaymentStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "payment_transactions")
public class PaymentTransaction extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @ToString.Exclude
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_id")
    @ToString.Exclude
    private UserSubscription subscription;

    @Column(name = "amount", nullable = false)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false)
    private String currency;

    @Column(name = "payment_Provider", nullable = false)
    @Enumerated(EnumType.STRING)
    private PaymentProvider paymentProvider; // momo, paypal, etc.

    @Column(name = "payment_method", nullable = false)
    private String paymentMethod; //card, bank, etc.

    @Column(name = "payment_gateway_id", nullable = false)
    private String paymentGatewayId; // e.g., vnp_TxnRef, stripePaymentIntentId

    @Column(name = "payment_gateway_order_id")
    private String paymentGatewayOrderId;

    @Column(name = "provider_status", nullable = false)
    private String providerStatus; // e.g., VNPAY_00, STRIPE_SUCCEEDED

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
    private String errorCode; // e.g., VNPAY_51, STRIPE_card_error

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "ipn_url")
    private String ipnUrl; // only vnpay?

    @Column(name = "return_url")
    private String returnUrl; // only vnpay?

    @Column(name = "description")
    private String description;

    @Column(name = "payment_details", columnDefinition = "TEXT")
    private String paymentDetails; // JSON string for provider-specific data
} 