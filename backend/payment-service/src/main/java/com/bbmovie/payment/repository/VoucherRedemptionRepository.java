package com.bbmovie.payment.repository;

import com.bbmovie.payment.entity.Voucher;
import com.bbmovie.payment.entity.VoucherRedemption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface VoucherRedemptionRepository extends JpaRepository<VoucherRedemption, UUID> {
    Optional<VoucherRedemption> findByVoucherAndUserId(Voucher voucher, String userId);
    long countByVoucherAndUserId(Voucher voucher, String userId);
}


