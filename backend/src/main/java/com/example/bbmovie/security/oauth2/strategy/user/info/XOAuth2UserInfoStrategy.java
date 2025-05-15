package com.example.bbmovie.security.oauth2.strategy.user.info;

import com.example.bbmovie.entity.enumerate.AuthProvider;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class XOAuth2UserInfoStrategy implements OAuth2UserInfoStrategy {

    @Override
    public String getEmailAttributeKey(Map<String, Object> attributes) {
        return "email";
    }

    @Override
    public String getEmail(Map<String, Object> attributes) {
        return (String) attributes.get("email");
    }

    @Override
    public String getNameAttributeKey(Map<String, Object> attributes) {
        return "global_name";
    }

    @Override
    public String getName(Map<String, Object> attributes) {
        return (String) attributes.get("global_name");
    }

    @Override
    public String getUsernameAttributeKey(Map<String, Object> attributes) {
        return "id";
    }

    @Override
    public String getUsername(Map<String, Object> attributes) {
        return (String) attributes.get("id");
    }

    @Override
    public String getAvatarUrlAttributeKey(Map<String, Object> attributes) {
        return "avatar";
    }

    @Override
    public String getAvatarUrl(Map<String, Object> attributes) {
        return (String) attributes.get("avatar");
    }

    @Override
    public AuthProvider getAuthProvider() {
        return AuthProvider.X;
    }
}
