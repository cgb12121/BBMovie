package com.example.bbmovie.repository;

import com.example.bbmovie.model.PaymentTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentTransactionRepository extends BaseRepository<PaymentTransaction> {
    Page<PaymentTransaction> findByUserId(Long userId, Pageable pageable);
    
    Optional<PaymentTransaction> findByPaymentGatewayId(String paymentGatewayId);
    
    Optional<PaymentTransaction> findByPaymentGatewayOrderId(String paymentGatewayOrderId);
    
    @Query("SELECT pt FROM PaymentTransaction pt WHERE pt.user.id = :userId AND pt.status = :status")
    List<PaymentTransaction> findByUserIdAndStatus(@Param("userId") Long userId, 
                                                 @Param("status") PaymentTransaction.TransactionStatus status);
    
    @Query("SELECT pt FROM PaymentTransaction pt WHERE pt.transactionDate BETWEEN :startDate AND :endDate")
    List<PaymentTransaction> findByTransactionDateBetween(@Param("startDate") LocalDateTime startDate, 
                                                        @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT SUM(pt.amount) FROM PaymentTransaction pt WHERE pt.status = 'COMPLETED' AND pt.transactionDate BETWEEN :startDate AND :endDate")
    BigDecimal calculateTotalRevenue(@Param("startDate") LocalDateTime startDate, 
                                   @Param("endDate") LocalDateTime endDate);
} 