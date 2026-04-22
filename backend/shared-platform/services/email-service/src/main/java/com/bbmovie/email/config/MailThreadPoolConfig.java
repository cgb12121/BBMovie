package com.bbmovie.email.config;

import lombok.extern.log4j.Log4j2;
import org.springframework.boot.autoconfigure.condition.ConditionalOnJava;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.system.JavaVersion;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.VirtualThreadTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.concurrent.DelegatingSecurityContextExecutor;

import java.util.concurrent.Executor;

@Log4j2
@Configuration
public class MailThreadPoolConfig {

    private static final String VIRTUAL_THREAD_PREFIX = "mail_virtual_thread_";
    private static final String PLATFORM_THREAD_PREFIX = "mail_task_executor_";

    @Bean(name = "emailExecutor")
    @ConditionalOnJava(range = ConditionalOnJava.Range.EQUAL_OR_NEWER, value = JavaVersion.TWENTY_ONE)
    public Executor emailExecutor() {
        log.info("Current Java version is {}", JavaVersion.getJavaVersion());
        log.info("Initializing virtual email executor");
        VirtualThreadTaskExecutor virtualThreadExecutor = new VirtualThreadTaskExecutor(VIRTUAL_THREAD_PREFIX);
        return new DelegatingSecurityContextExecutor(virtualThreadExecutor);
    }

    @Bean(name = "emailExecutor")
    @ConditionalOnMissingBean
    @ConditionalOnJava(range = ConditionalOnJava.Range.OLDER_THAN, value = JavaVersion.TWENTY_ONE)
    public Executor emailExecutorFallback() {
        log.info("No Bean of emailExecutor found. Fallback to email executor");
        log.info("CURRENT JAVA VERSION: {} does not support virtual thread.", JavaVersion.getJavaVersion());
        log.info("Initialize ThreadPoolTaskExecutor...");
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(50);
        executor.setMaxPoolSize(100);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix(PLATFORM_THREAD_PREFIX);
        return new DelegatingSecurityContextExecutor(executor);
    }
}