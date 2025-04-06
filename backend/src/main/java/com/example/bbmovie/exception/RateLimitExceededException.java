package com.example.bbmovie.exception;

public class RateLimitExceededException extends RuntimeException {
     public RateLimitExceededException(String massage) {
          super(massage);
     }
}
