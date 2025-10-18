package com.bbmovie.fileservice.service.concurrency;

import jakarta.annotation.PreDestroy;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Log4j2
@Service
public class PrioritizedTaskExecutor {

    private final ThreadPoolExecutor executor;

    public PrioritizedTaskExecutor() {
        int corePoolSize = 5; // Max 5 concurrent uploads
        int maxPoolSize = 5;  // Fixed size pool
        long keepAliveTime = 0L;

        // This is the key: using a PriorityBlockingQueue
        this.executor = new ThreadPoolExecutor(
                corePoolSize,
                maxPoolSize,
                keepAliveTime,
                TimeUnit.MILLISECONDS,
                new PriorityBlockingQueue<>()
        );
        log.info("Initialized PrioritizedTaskExecutor with {} worker threads.", corePoolSize);
    }

    public void submit(Runnable task, TaskPriority priority) {
        log.info("Submitting task with priority: {}. Queue size: {}", priority, executor.getQueue().size());
        this.executor.execute(new PrioritizedTask(task, priority));
    }

    @PreDestroy
    public void shutdown() {
        log.info("Shutting down PrioritizedTaskExecutor.");
        executor.shutdown();
        try {
            if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
