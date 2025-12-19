package com.bbmovie.transcodeworker.exception;

/**
 * Custom exception class for unsupported file extension errors.
 * Thrown when a file with an unsupported extension is attempted to be uploaded.
 */
public class UnsupportedExtension extends RuntimeException {

    /**
     * Constructs a new UnsupportedExtension exception with the specified detail message.
     *
     * @param message the detail message explaining the cause of the exception
     */
    public UnsupportedExtension(String message) {
        super(message);
    }
}
