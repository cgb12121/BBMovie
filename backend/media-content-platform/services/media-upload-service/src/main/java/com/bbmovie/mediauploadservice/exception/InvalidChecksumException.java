package com.bbmovie.mediauploadservice.exception;

public class InvalidChecksumException extends RuntimeException {
    public InvalidChecksumException(String message) {
        super(message);
    }
}
