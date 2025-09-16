package com.bbmovie.payment.service.job;

import com.bbmovie.payment.service.UserSubscriptionService;
import com.bbmovie.payment.repository.UserSubscriptionRepository;
import com.bbmovie.payment.entity.UserSubscription;
import com.example.common.dtos.nats.SubscriptionEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.nats.client.Connection;
import io.nats.client.JetStream;
import lombok.extern.log4j.Log4j2;

import java.time.LocalDateTime;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Log4j2
@Service
public class SubscriptionRenewalJob implements Job {

    private final UserSubscriptionService userSubscriptionService;
    private final UserSubscriptionRepository userSubscriptionRepository;
    private final JetStream js;
    private final ObjectMapper objectMapper;

    @Autowired
    public SubscriptionRenewalJob(
            UserSubscriptionService userSubscriptionService,
            UserSubscriptionRepository userSubscriptionRepository,
            Connection nats,
            ObjectMapper objectMapper
    ) throws java.io.IOException {
        this.userSubscriptionService = userSubscriptionService;
        this.userSubscriptionRepository = userSubscriptionRepository;
        this.js = nats.jetStream();
        this.objectMapper = objectMapper;
    }

    //TODO: implement
    @Override
    public void execute(JobExecutionContext context) {
        log.info("Running subscription renewal job..., {}", context.getJobDetail());
        userSubscriptionService.processDueSubscriptions();

        // Notify upcoming renewals (next 3 days)
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime in3Days = now.plusDays(3);
        for (UserSubscription sub : userSubscriptionRepository
                .findByIsActiveTrueAndAutoRenewTrueAndNextPaymentDateBetween(now, in3Days)) {
            try {
                SubscriptionEvent event = new SubscriptionEvent("RENEWAL_UPCOMING", sub.getUserId(), null,
                        sub.getPlan() != null ? sub.getPlan().getName() : null,
                        sub.getNextPaymentDate());
                byte[] payload = objectMapper.writeValueAsBytes(event);
                js.publish("payments.subscription.renewal_upcoming", payload);
            } catch (Exception e) {
                log.error("Failed to publish renewal upcoming event for sub {}", sub.getId(), e);
            }
        }
    }
}