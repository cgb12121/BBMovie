package com.bbmovie.payment.service.job;

import com.bbmovie.payment.service.UserSubscriptionService;
import com.bbmovie.payment.repository.UserSubscriptionRepository;
import com.bbmovie.payment.entity.UserSubscription;
import com.bbmovie.payment.service.nats.AbstractNatsJetStreamService;
import com.bbmovie.common.dtos.nats.SubscriptionEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.log4j.Log4j2;

import java.io.IOException;
import java.time.LocalDateTime;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Log4j2
@Service
public class SubscriptionRenewalJob extends AbstractNatsJetStreamService implements Job {

    private final UserSubscriptionService userSubscriptionService;
    private final UserSubscriptionRepository userSubscriptionRepository;
    private final ObjectMapper objectMapper;

    @Autowired
    public SubscriptionRenewalJob(
            UserSubscriptionService userSubscriptionService,
            UserSubscriptionRepository userSubscriptionRepository,
            ObjectMapper objectMapper) throws IOException {
        this.userSubscriptionService = userSubscriptionService;
        this.userSubscriptionRepository = userSubscriptionRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public void execute(JobExecutionContext context) {
        log.info("Running subscription renewal job..., {}", context.getJobDetail());
        userSubscriptionService.processDueSubscriptions();

        // Notify upcoming renewals (next 3 days)
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime in3Days = now.plusDays(3);

        for (UserSubscription sub : userSubscriptionRepository.findByIsActiveTrueAndAutoRenewTrueAndNextPaymentDateBetween(now, in3Days)) {
            try {
                SubscriptionEvent event = new SubscriptionEvent(
                        sub.getUserId(),
                        sub.getUserEmail(),
                        sub.getPlan() != null
                                ? sub.getPlan().getName()
                                : null,
                        sub.getNextPaymentDate()
                );
                byte[] payload = objectMapper.writeValueAsBytes(event);
                getJetStream().publish("payments.subscription.renewal.upcoming", payload);
            } catch (Exception e) {
                log.error("Failed to publish renewal upcoming event for sub {}", sub.getId(), e);
            }
        }
    }
}