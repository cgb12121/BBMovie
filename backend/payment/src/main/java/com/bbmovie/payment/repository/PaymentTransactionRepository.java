package com.bbmovie.payment.repository;

import com.bbmovie.payment.entity.PaymentTransaction;
import com.bbmovie.payment.entity.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, UUID> {
    Optional<PaymentTransaction> findByProviderTransactionId(String providerTransactionId);

    List<PaymentTransaction> findByStatusAndExpiresAtBefore(PaymentStatus status, LocalDateTime cutoff);

    List<PaymentTransaction> findTop100ByUserIdAndStatusOrderByTransactionDateDesc(String userId, PaymentStatus status);

    List<PaymentTransaction> findByStatusAndTransactionDateAfter(PaymentStatus status, LocalDateTime after);
}
