package com.bbmovie.email.utils;

public class EmailUtils {
    private EmailUtils() {}

    public static String createVerificationEmailUrl(String frontEndUrl, String verificationToken) {
        return frontEndUrl + "/verify-email?token=" + verificationToken;
    }

    public static String createResetPasswordEmailUrl(String frontEndUrl, String resetToken) {
        return frontEndUrl + "/reset-password?token=" + resetToken;
    }
}
