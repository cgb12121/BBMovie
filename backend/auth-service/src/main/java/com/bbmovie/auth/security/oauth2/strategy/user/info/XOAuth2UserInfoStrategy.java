package com.bbmovie.auth.security.oauth2.strategy.user.info;

import com.bbmovie.auth.entity.enumerate.AuthProvider;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Spring does not support nested JSON objects, so we need to extract the data from the "data" key.
 */
@Component
@SuppressWarnings("unchecked")
public class XOAuth2UserInfoStrategy implements OAuth2UserInfoStrategy {

    @Override
    public String getEmailAttributeKey(Map<String, Object> attributes) {
        return "data"; // Top-level key used in DefaultOAuth2User
    }

    @Override
    public String getEmail(Map<String, Object> attributes) {
        Map<String, Object> data = (Map<String, Object>) attributes.get("data");
        return data != null ? data.get("username") + "x.bbmovie@gmail.com" : null;
    }

    @Override
    public String getNameAttributeKey(Map<String, Object> attributes) {
        return "data";
    }

    @Override
    public String getName(Map<String, Object> attributes) {
        Map<String, Object> data = (Map<String, Object>) attributes.get("data");
        return data != null ? (String) data.get("name") : null;
    }

    @Override
    public String getUsernameAttributeKey(Map<String, Object> attributes) {
        return "data";
    }

    @Override
    public String getUsername(Map<String, Object> attributes) {
        Map<String, Object> data = (Map<String, Object>) attributes.get("data");
        return data != null ? (String) data.get("username") : null;
    }

    @Override
    public String getAvatarUrlAttributeKey(Map<String, Object> attributes) {
        return "data";
    }

    @Override
    public String getAvatarUrl(Map<String, Object> attributes) {
        Map<String, Object> data = (Map<String, Object>) attributes.get("data");
        return data != null ? (String) data.get("profile_image_url") : null;
    }

    @Override
    public AuthProvider getAuthProvider() {
        return AuthProvider.X;
    }
}
