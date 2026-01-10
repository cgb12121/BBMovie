package com.bbmovie.mediastreamingservice.exception;

/**
 * Exception thrown when a user attempts to access content they don't have permission for.
 */
public class AccessDeniedException extends RuntimeException {
    
    public AccessDeniedException(String message) {
        super(message);
    }
    
    public AccessDeniedException(String message, Throwable cause) {
        super(message, cause);
    }
}
