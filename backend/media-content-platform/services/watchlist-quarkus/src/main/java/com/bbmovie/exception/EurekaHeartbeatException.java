package com.bbmovie.exception;

public class EurekaHeartbeatException extends Exception {
    public EurekaHeartbeatException(String message) {
        super(message);
    }
    public EurekaHeartbeatException(String message, Throwable cause) {
        super(message, cause);
    }
}