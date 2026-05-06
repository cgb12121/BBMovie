package bbmovie.transcode.temporal_orchestrator.temporal;

import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;

import java.time.Duration;

public final class TemporalPolicies {

    public static ActivityOptions analyzerOptions() {
        return ActivityOptions.newBuilder()
                .setStartToCloseTimeout(Duration.ofMinutes(5))
                .setRetryOptions(RetryOptions.newBuilder()
                        .setMaximumAttempts(5)
                        .setBackoffCoefficient(2.0)
                        .build())
                .build();
    }

    public static ActivityOptions encoderOptions() {
        return ActivityOptions.newBuilder()
                .setStartToCloseTimeout(Duration.ofHours(6))
                .setHeartbeatTimeout(Duration.ofMinutes(2))
                .setRetryOptions(RetryOptions.newBuilder()
                        .setMaximumAttempts(3)
                        .setBackoffCoefficient(2.0)
                        .build())
                .build();
    }

    public static ActivityOptions qualityOptions() {
        return ActivityOptions.newBuilder()
                .setStartToCloseTimeout(Duration.ofHours(2))
                .setHeartbeatTimeout(Duration.ofMinutes(2))
                .setRetryOptions(RetryOptions.newBuilder()
                        .setMaximumAttempts(3)
                        .setBackoffCoefficient(2.0)
                        .build())
                .build();
    }

    public static ActivityOptions subtitleOptions() {
        return ActivityOptions.newBuilder()
                .setStartToCloseTimeout(Duration.ofMinutes(30))
                .setRetryOptions(RetryOptions.newBuilder()
                        .setMaximumAttempts(3)
                        .setBackoffCoefficient(2.0)
                        .build())
                .build();
    }

    private TemporalPolicies() {
    }
}
