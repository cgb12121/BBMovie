package com.bbmovie.payment.config;

import com.bbmovie.payment.service.SubscriptionRenewalJob;
import org.quartz.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class QuartzConfig {

    @Bean(name = "subscriptionJobDetail")
    public JobDetail subscriptionJobDetail() {
        return JobBuilder.newJob(SubscriptionRenewalJob.class)
                .withIdentity("subscriptionRenewalJob")
                .storeDurably()
                .build();
    }

    @Bean
    public Trigger subscriptionTrigger(@Qualifier("subscriptionJobDetail") JobDetail subscriptionJobDetail) {
        return TriggerBuilder.newTrigger()
                .forJob(subscriptionJobDetail)
                .withIdentity("subscriptionTrigger")
                .withSchedule(CronScheduleBuilder.dailyAtHourAndMinute(0, 0)) // 00:00 every day
                .build();
    }
}
