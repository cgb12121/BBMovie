package com.bbmovie.auth.exception;

public class AccountLockedException extends AuthenticationException {
    public AccountLockedException(String message) {
        super(message);
    }
} 