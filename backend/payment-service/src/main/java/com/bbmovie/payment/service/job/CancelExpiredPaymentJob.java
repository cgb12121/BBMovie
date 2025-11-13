package com.bbmovie.payment.service.job;

import com.bbmovie.payment.entity.PaymentTransaction;
import com.bbmovie.payment.entity.enums.PaymentStatus;
import com.bbmovie.payment.repository.PaymentTransactionRepository;
import lombok.extern.log4j.Log4j2;

import java.time.LocalDateTime;
import java.util.List;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Log4j2
@Service
public class CancelExpiredPaymentJob implements Job {

    private final PaymentTransactionRepository paymentTransactionRepository;

    @Autowired
    public CancelExpiredPaymentJob(PaymentTransactionRepository paymentTransactionRepository) {
        this.paymentTransactionRepository = paymentTransactionRepository;
    }

    @Override
    public void execute(JobExecutionContext jobExecutionContext) {
        log.info("Running cancel expired payment job..., {}", jobExecutionContext.getJobDetail());
        LocalDateTime now = LocalDateTime.now();
        List<PaymentTransaction> expired = paymentTransactionRepository
                .findByStatusAndExpiresAtBefore(PaymentStatus.PENDING, now);
        for (PaymentTransaction txn : expired) {
            if (txn.getStatus() == PaymentStatus.PENDING) {
                txn.setStatus(PaymentStatus.CANCELLED);
                txn.setCancelDate(now);
                txn.setStatus(PaymentStatus.AUTO_CANCELLED);
            }
        }
        if (!expired.isEmpty()) {
            paymentTransactionRepository.saveAll(expired);
            log.info("Auto-cancelled {} expired pending transactions", expired.size());
        }
    }
}
