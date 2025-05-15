package com.example.bbmovie.security.oauth2.strategy.user.info;

import com.example.bbmovie.entity.enumerate.AuthProvider;

import java.util.Map;

public interface OAuth2UserInfoStrategy {
    String getEmailAttributeKey(Map<String, Object> attributes);
    String getEmail(Map<String, Object> attributes);

    String getNameAttributeKey(Map<String, Object> attributes);
    String getName(Map<String, Object> attributes);

    String getUsernameAttributeKey(Map<String, Object> attributes);
    String getUsername(Map<String, Object> attributes);

    String getAvatarUrlAttributeKey(Map<String, Object> attributes);
    String getAvatarUrl(Map<String, Object> attributes);

    AuthProvider getAuthProvider();
}
