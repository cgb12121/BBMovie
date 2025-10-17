package com.bbmovie.auth.constant;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@SuppressWarnings("unused")
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class AuthErrorMessages {
    public static final String INVALID_CREDENTIALS = "Invalid credentials";
    public static final String ACCOUNT_LOCKED = "Account is locked";
    public static final String ACCOUNT_DISABLED = "Account is disabled";
    public static final String EMAIL_NOT_VERIFIED = "Email is not verified";
    public static final String EMAIL_ALREADY_EXISTS = "This email already used, please try another one.";
    public static final String EMAIL_ALREADY_VERIFIED = "Email is already verified";
    public static final String EMAIL_SENDING_ERROR = "Error occurred while sending email";
    public static final String INVALID_TOKEN = "Invalid token";
    public static final String TOKEN_EXPIRED = "Token has expired";
}
