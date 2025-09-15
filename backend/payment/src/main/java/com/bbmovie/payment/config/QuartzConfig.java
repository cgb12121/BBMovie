package com.bbmovie.payment.config;

import com.bbmovie.payment.service.job.CancelExpiredPaymentJob;
import com.bbmovie.payment.service.job.SubscriptionRenewalJob;
import com.bbmovie.payment.service.job.DailyReconciliationJob;
import com.bbmovie.payment.service.job.DailyReportJob;
import org.quartz.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.TimeZone;

@Configuration
public class QuartzConfig {

    private static final String DEFAULT_TIMEZONE = "Asia/Ho_Chi_Minh";

    @Bean(name = "subscriptionJobDetail")
    public JobDetail subscriptionJobDetail() {
        return JobBuilder.newJob(SubscriptionRenewalJob.class)
                .withIdentity("subscriptionRenewalJob")
                .storeDurably()
                .build();
    }

    @Bean(name = "cancelPaymentJobDetail")
    public JobDetail cancelPaymentJobDetail() {
        return JobBuilder.newJob(CancelExpiredPaymentJob.class)
                .withIdentity("cancelExpiredPaymentJob")
                .storeDurably()
                .build();
    }

    @Bean(name = "reconcileJobDetail")
    public JobDetail reconcileJobDetail() {
        return JobBuilder.newJob(DailyReconciliationJob.class)
                .withIdentity("dailyReconciliationJob")
                .storeDurably()
                .build();
    }

    @Bean(name = "dailyReportJobDetail")
    public JobDetail dailyReportJobDetail() {
        return JobBuilder.newJob(DailyReportJob.class)
                .withIdentity("dailyReportJob")
                .storeDurably()
                .build();
    }

    @Bean(name = "cancelPaymentTrigger")
    public Trigger cancelPaymentTrigger(@Qualifier("cancelPaymentJobDetail") JobDetail cancelPaymentJobDetail) {
        return TriggerBuilder.newTrigger()
                .forJob(cancelPaymentJobDetail)
                .withIdentity("cancelPaymentTrigger")
                .withSchedule(CronScheduleBuilder
                        .cronSchedule("0 * * * *") // every hour
                        .inTimeZone(TimeZone.getTimeZone(DEFAULT_TIMEZONE))
                )
                .build();
    }

    @Bean(name = "subscriptionTrigger")
    public Trigger subscriptionTrigger(@Qualifier("subscriptionJobDetail") JobDetail subscriptionJobDetail) {
        return TriggerBuilder.newTrigger()
                .forJob(subscriptionJobDetail)
                .withIdentity("subscriptionTrigger")
                .withSchedule(CronScheduleBuilder
                        .dailyAtHourAndMinute(0, 0) // 00:00 every day
                        .inTimeZone(TimeZone.getTimeZone(DEFAULT_TIMEZONE))
                )
                .build();
    }

    @Bean(name = "reconcileTrigger")
    public Trigger reconcileTrigger(@Qualifier("reconcileJobDetail") JobDetail reconcileJobDetail) {
        return TriggerBuilder.newTrigger()
                .forJob(reconcileJobDetail)
                .withIdentity("reconcileTrigger")
                .withSchedule(CronScheduleBuilder
                        .dailyAtHourAndMinute(23, 55)
                        .inTimeZone(TimeZone.getTimeZone(DEFAULT_TIMEZONE))
                )
                .build();
    }

    @Bean(name = "dailyReportTrigger")
    public Trigger dailyReportTrigger(@Qualifier("dailyReportJobDetail") JobDetail dailyReportJobDetail) {
        return TriggerBuilder.newTrigger()
                .forJob(dailyReportJobDetail)
                .withIdentity("dailyReportTrigger")
                .withSchedule(CronScheduleBuilder
                        .dailyAtHourAndMinute(23, 59)
                        .inTimeZone(TimeZone.getTimeZone(DEFAULT_TIMEZONE))
                )
                .build();
    }
}