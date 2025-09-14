package com.bbmovie.payment.service;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class SubscriptionRenewalJob implements Job {

    private final UserSubscriptionService subscriptionService;

    @Autowired
    public SubscriptionRenewalJob(UserSubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    @Override
    public void execute(JobExecutionContext context) {
        subscriptionService.processDueSubscriptions();
    }
}
