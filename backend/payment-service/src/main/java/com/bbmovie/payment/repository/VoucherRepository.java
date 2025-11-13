package com.bbmovie.payment.repository;

import com.bbmovie.payment.entity.Voucher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface VoucherRepository extends JpaRepository<Voucher, UUID> {
    Optional<Voucher> findByCodeIgnoreCase(String code);
}


