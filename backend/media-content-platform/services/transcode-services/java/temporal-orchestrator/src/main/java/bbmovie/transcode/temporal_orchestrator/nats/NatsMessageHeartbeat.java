package bbmovie.transcode.temporal_orchestrator.nats;

import io.nats.client.Message;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@ConditionalOnBean(io.nats.client.Connection.class)
public class NatsMessageHeartbeat {

    @Value("${nats.consumer.heartbeat-interval-seconds:30}")
    private int heartbeatIntervalSeconds;

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    private final Map<String, ScheduledFuture<?>> active = new ConcurrentHashMap<>();

    public Handle start(Message message, String taskId) {
        String id = taskId + "_" + System.nanoTime();
        ScheduledFuture<?> future = scheduler.scheduleAtFixedRate(() -> {
            try {
                message.inProgress();
            } catch (Exception e) {
                log.warn("Heartbeat failed for {}", taskId, e);
            }
        }, heartbeatIntervalSeconds, heartbeatIntervalSeconds, TimeUnit.SECONDS);
        active.put(id, future);
        return new Handle(id, message);
    }

    public void stopAndAck(Handle handle) {
        cancel(handle);
        if (handle != null && handle.message() != null) {
            try {
                handle.message().ack();
            } catch (Exception e) {
                log.error("ACK failed: {}", e.getMessage());
            }
        }
    }

    public void stopAndNak(Handle handle) {
        cancel(handle);
        if (handle != null && handle.message() != null) {
            try {
                handle.message().nak();
            } catch (Exception e) {
                log.error("NAK failed: {}", e.getMessage());
            }
        }
    }

    private void cancel(Handle handle) {
        if (handle == null) {
            return;
        }
        ScheduledFuture<?> f = active.remove(handle.id());
        if (f != null) {
            f.cancel(false);
        }
    }

    public void shutdown() {
        active.values().forEach(f -> f.cancel(false));
        active.clear();
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    public record Handle(String id, Message message) {
    }
}
