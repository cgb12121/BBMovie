package com.example.bbmovie.service;

import com.example.bbmovie.dto.response.AuthResponse;
import com.example.bbmovie.model.User;
import com.example.bbmovie.repository.UserRepository;
import com.example.bbmovie.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
@RequiredArgsConstructor
public class OAuth2Service {

    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;

    public AuthResponse loginViaGoogle(String email, String name) {
        User user = userRepository.findByEmail(email)
                .orElseGet(() -> {
                    User newUser = new User();
                    newUser.setEmail(email);
                    assert email != null;
                    newUser.setUsername(email.split("@")[0]);
                    assert name != null;
                    newUser.setFirstName(name.split(" ")[0]);
                    newUser.setLastName(name.split(" ").length > 1 ? name.split(" ")[1] : "");
                    newUser.setRoles(Collections.singleton("ROLE_USER"));
                    newUser.setIsEnabled(true);
                    return userRepository.save(newUser);
                });

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, null, Collections.singleton(user.getAuthorities().iterator().next()))
        );

        String accessToken = jwtTokenProvider.generateAccessToken(authentication);
        String refreshToken = jwtTokenProvider.generateRefreshToken(authentication);

        return AuthResponse.builder()
                .email(user.getEmail())
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .role(user.getRoles().toString())
                .build();
    }
}
