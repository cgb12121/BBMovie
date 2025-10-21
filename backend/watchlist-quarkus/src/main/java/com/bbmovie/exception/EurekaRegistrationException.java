package com.bbmovie.exception;

public class EurekaRegistrationException extends Exception {
        public EurekaRegistrationException(String message) {
            super(message);
        }
        public EurekaRegistrationException(String message, Throwable cause) {
            super(message, cause);
        }
}