package com.example.common.dtos.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemHealthEvent {
    private String service;
    private Status status;
    private String reason;

    public enum Status {
        UP, DOWN
    }

    public static SystemHealthEvent up(String service, String reason) {
        return SystemHealthEvent.builder()
                .service(service)
                .status(Status.UP)
                .reason(reason)
                .build();
    }

    public static SystemHealthEvent down(String service, String reason) {
        return SystemHealthEvent.builder()
                .service(service)
                .status(Status.DOWN)
                .reason(reason)
                .build();
    }
}
