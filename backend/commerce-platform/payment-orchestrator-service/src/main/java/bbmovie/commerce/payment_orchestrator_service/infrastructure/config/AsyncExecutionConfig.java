package bbmovie.commerce.payment_orchestrator_service.infrastructure.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.support.TaskExecutorAdapter;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
@EnableAsync
@EnableTransactionManagement
public class AsyncExecutionConfig {

    @Bean(destroyMethod = "shutdown")
    public ExecutorService virtualThreadExecutorService() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }

    @Bean(name = "virtualTaskExecutor")
    public AsyncTaskExecutor virtualTaskExecutor(ExecutorService virtualThreadExecutorService) {
        return new TaskExecutorAdapter(virtualThreadExecutorService);
    }

    @Bean(name = "ioBoundTaskExecutor")
    public ThreadPoolTaskExecutor ioBoundTaskExecutor(
            @Value("${app.thread-pool.io.core-size:8}") int coreSize,
            @Value("${app.thread-pool.io.max-size:32}") int maxSize,
            @Value("${app.thread-pool.io.queue-capacity:1000}") int queueCapacity,
            @Value("${app.thread-pool.io.keep-alive-seconds:60}") int keepAliveSeconds
    ) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setThreadNamePrefix("payment-io-");
        executor.setCorePoolSize(coreSize);
        executor.setMaxPoolSize(maxSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setKeepAliveSeconds(keepAliveSeconds);
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(20);
        executor.initialize();
        return executor;
    }
}

