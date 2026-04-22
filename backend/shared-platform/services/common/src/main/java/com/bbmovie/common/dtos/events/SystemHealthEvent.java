package com.bbmovie.common.dtos.events;

public class SystemHealthEvent {

    private String service;
    private Status status;
    private String reason;

    public enum Status {
        UP, DOWN
    }

    public SystemHealthEvent() {}

    public SystemHealthEvent(String service, Status status, String reason) {
        this.service = service;
        this.status = status;
        this.reason = reason;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String service;
        private Status status;
        private String reason;

        public Builder service(String service) {
            this.service = service;
            return this;
        }

        public Builder status(Status status) {
            this.status = status;
            return this;
        }

        public Builder reason(String reason) {
            this.reason = reason;
            return this;
        }

        public SystemHealthEvent build() {
            return new SystemHealthEvent(service, status, reason);
        }
    }

    public static SystemHealthEvent up(String service, String reason) {
        return new SystemHealthEvent(service, Status.UP, reason);
    }

    public static SystemHealthEvent down(String service, String reason) {
        return new SystemHealthEvent(service, Status.DOWN, reason);
    }

    public String getService() { return service; }
    public void setService(String service) { this.service = service; }

    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
