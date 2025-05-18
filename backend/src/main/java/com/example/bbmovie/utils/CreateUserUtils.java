package com.example.bbmovie.utils;

import com.example.bbmovie.entity.User;
import com.example.bbmovie.entity.enumerate.Role;
import com.example.bbmovie.security.oauth2.strategy.user.info.OAuth2UserInfoStrategy;
import org.apache.commons.lang3.RandomStringUtils;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Random;

public class CreateUserUtils {
    private static final Random random = new SecureRandom();

    public static User createUserForOauth2(
            String email, String encodedPassword, String avatarUrl, String firstName, String lastName,
            OAuth2UserInfoStrategy strategy, Role role
    ) {
        return User.builder()
                .email(email)
                .username(email)
                .password(encodedPassword)
                .profilePictureUrl(avatarUrl)
                .firstName(firstName)
                .lastName(lastName)
                .role(role)
                .isAccountNonExpired(true)
                .isAccountNonLocked(true)
                .isCredentialsNonExpired(true)
                .isEnabled(true)
                .lastLoginTime(LocalDateTime.now())
                .authProvider(strategy.getAuthProvider())
                .build();
    }

    public static String generateRandomPasswordFoForOauth2() {
        return RandomStringUtils.random(20, 0, 0, true,true, null, random);
    }

    public static String generateRandomUsername() {
        return RandomStringUtils.random(10, 0, 0, true,true, null, random);
    }

    public static String generateRandomEmail() {
        return RandomStringUtils.random(10, 0, 0, true,true, null, random) + "@bbmovie.com";
    }

    public static String generateRandomAvatarUrl() {
        int from1to1084cuzFreeApiHas1084img = new Random().nextInt(1084) + 1;
        return "https://picsum.photos/id/" + from1to1084cuzFreeApiHas1084img + "/200/200";
    }

    public static String generateDefaultProfilePictureUrl() {
        return "https://cdn.pixabay.com/photo/2023/02/18/11/00/icon-7797704_1280.png";
    }
}
