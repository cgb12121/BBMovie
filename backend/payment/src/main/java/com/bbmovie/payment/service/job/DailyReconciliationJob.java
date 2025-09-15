package com.bbmovie.payment.service.job;

import com.bbmovie.payment.entity.PaymentTransaction;
import com.bbmovie.payment.entity.enums.PaymentProvider;
import com.bbmovie.payment.entity.enums.PaymentStatus;
import com.bbmovie.payment.repository.PaymentTransactionRepository;
import com.bbmovie.payment.service.PaymentProviderAdapter;
import lombok.extern.log4j.Log4j2;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static com.bbmovie.payment.entity.enums.PaymentProvider.*;

@Log4j2
@Service
public class DailyReconciliationJob implements Job {

    private final Map<String, PaymentProviderAdapter> providers;
    private final PaymentTransactionRepository paymentTransactionRepository;

    @Autowired
    public DailyReconciliationJob(Map<String, PaymentProviderAdapter> providers,
                                  PaymentTransactionRepository paymentTransactionRepository) {
        this.providers = providers;
        this.paymentTransactionRepository = paymentTransactionRepository;
    }

    @Override
    public void execute(JobExecutionContext context) {
        log.info("Running daily reconciliation job..., {}", context.getJobDetail());
        LocalDateTime since = LocalDateTime.now().minusDays(1);
        List<PaymentTransaction> toCheck = paymentTransactionRepository
                .findByStatusAndTransactionDateAfter(PaymentStatus.PENDING, since);
        for (PaymentTransaction txn : toCheck) {
            String providerKey = switch (txn.getPaymentProvider()) {
                case VNPAY -> VNPAY.getName();
                case MOMO -> MOMO.getName();
                case ZALOPAY -> ZALOPAY.getName();
                case STRIPE -> STRIPE.getName();
                case PAYPAL -> PAYPAL.getName();
            };
            PaymentProviderAdapter adapter = providers.get(providerKey);
            try {
                adapter.queryPayment("SYSTEM", txn.getProviderTransactionId());
            } catch (Exception e) {
                log.error("Reconciliation query failed for {} {}", txn.getId(), e.getMessage());
            }
        }
    }
}


