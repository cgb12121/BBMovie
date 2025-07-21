package com.bbmovie.auth.constant.error;

@SuppressWarnings("unused")
public class UserErrorMessages {
    public static final String USER_NOT_FOUND = "User not found with id: %d";
    public static final String USER_NOT_FOUND_BY_EMAIL = "No user found with email: %s";
    public static final String USER_ALREADY_EXISTS = "User already exists with email: %s";
    public static final String USER_ALREADY_VERIFIED = "User is already verified";
    public static final String USER_SENDING_ERROR = "Error occurred while sending email";
    public static final String INVALID_TOKEN = "Invalid token";
    public static final String TOKEN_EXPIRED = "Token has expired";

    private UserErrorMessages() {}
}
