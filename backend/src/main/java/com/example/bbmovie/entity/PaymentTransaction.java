package com.example.bbmovie.entity;

import com.example.bbmovie.entity.base.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payment_transactions")
@Getter
@Setter
@ToString
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
    private String currency = "USD";

    @Column(name = "payment_method", nullable = false)
    @Enumerated(EnumType.STRING)
    private PaymentMethod paymentMethod;

    @Column(name = "payment_gateway_id")
    private String paymentGatewayId;

    @Column(name = "payment_gateway_order_id")
    private String paymentGatewayOrderId;

    @Column(name = "transaction_date", nullable = false)
    private LocalDateTime transactionDate;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private TransactionStatus status;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "ipn_url")
    private String ipnUrl;

    @Column(name = "return_url")
    private String returnUrl;

    @Column(name = "description")
    private String description;

    @Column(name = "payment_details", length = 4000)
    private String paymentDetails; // JSON string of payment details

    public enum TransactionStatus {
        PENDING,
        COMPLETED,
        FAILED,
        REFUNDED,
        CANCELLED
    }

    public enum PaymentMethod {
        PAYPAL,
        VNPAY
    }
} 