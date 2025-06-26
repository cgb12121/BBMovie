package com.example.bbmovie.exception;

public class AccountNotEnabledException extends AuthenticationException {
    public AccountNotEnabledException(String message) {
        super(message);
    }
} 