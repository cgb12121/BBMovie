package com.bbmovie.transcodeworker.exception;

/**
 * Custom exception class for file upload related errors.
 * Thrown when there are issues during the file upload process.
 */
public class FileUploadException extends RuntimeException {

    /**
     * Constructs a new FileUploadException with the specified detail message.
     *
     * @param message the detail message explaining the cause of the exception
     */
    public FileUploadException(String message) {
        super(message);
    }
}
