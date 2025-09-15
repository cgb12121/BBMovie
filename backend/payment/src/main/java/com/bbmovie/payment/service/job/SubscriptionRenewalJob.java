package com.bbmovie.payment.service.job;

import com.bbmovie.payment.service.UserSubscriptionService;
import lombok.extern.log4j.Log4j2;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Log4j2
@Service
public class SubscriptionRenewalJob implements Job {

    private final UserSubscriptionService userSubscriptionService;

    @Autowired
    public SubscriptionRenewalJob(UserSubscriptionService userSubscriptionService) {
        this.userSubscriptionService = userSubscriptionService;
    }

    //TODO: implement
    @Override
    public void execute(JobExecutionContext context) {
        log.info("Running subscription renewal job..., {}", context.getJobDetail());
        userSubscriptionService.processDueSubscriptions();
    }
}