package com.example.bbmovie.repository;

import com.example.bbmovie.entity.PaymentTransaction;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentTransactionRepository extends BaseRepository<PaymentTransaction> {

    Optional<PaymentTransaction> findByPaymentGatewayId(String paymentGatewayId);
}