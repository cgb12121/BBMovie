package com.bbmovie.payment.entity;

import com.bbmovie.payment.entity.base.BaseEntity;
import com.bbmovie.payment.entity.enums.PaymentProvider;
import com.bbmovie.payment.entity.enums.PaymentStatus;
import com.bbmovie.payment.service.converter.CurrencyUnitAttributeConverter;
import jakarta.persistence.*;
import lombok.*;

import javax.money.CurrencyUnit;
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

    @Convert(converter = CurrencyUnitAttributeConverter.class)
    @Column(name = "currency", nullable = false, length = 3)
    private CurrencyUnit currency;

    @Column(name = "payment_provider", nullable = false)
    @Enumerated(EnumType.STRING)
    private PaymentProvider paymentProvider;

    @Column(name = "payment_method")
    private String paymentMethod;

    @Column(name = "provider_transaction_id", columnDefinition = "TEXT")
    private String providerTransactionId;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "transaction_date", nullable = false)
    private LocalDateTime transactionDate;

    @Column(name = "cancel_date")
    private LocalDateTime cancelDate;

    @Column(name = "refund_date")
    private LocalDateTime refundDate;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private PaymentStatus status;

    @Column(name = "response_code")
    private String responseCode;

    @Column(name = "response_message", columnDefinition = "TEXT")
    private String responseMessage;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "payment_details", columnDefinition = "TEXT")
    private String paymentDetails;
}
