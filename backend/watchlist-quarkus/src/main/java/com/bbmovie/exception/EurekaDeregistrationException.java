
package com.bbmovie.exception;

public class EurekaDeregistrationException extends Exception {
    public EurekaDeregistrationException(String message) {
        super(message);
    }
    public EurekaDeregistrationException(String message, Throwable cause) {
        super(message, cause);
    }
}