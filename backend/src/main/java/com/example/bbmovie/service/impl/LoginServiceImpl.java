package com.example.bbmovie.service.impl;

import com.example.bbmovie.exception.*;
import com.example.bbmovie.model.User;
import com.example.bbmovie.repository.UserRepository;
import com.example.bbmovie.service.intf.LoginService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class LoginServiceImpl implements LoginService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    @Override
    public User login(String usernameOrEmail, String password) {
        User user = userRepository.findByUsernameOrEmail(usernameOrEmail, usernameOrEmail)
                .orElseThrow(() -> new UserNotFoundException("Invalid username/email or password"));

        boolean isUserEnabled = user.isEnabled();
        if (!isUserEnabled) {
            throw new AccountNotEnabledException("Account is not enabled. Please verify your email first.");
        }

        boolean correctPassword = passwordEncoder.matches(password, user.getPassword());
        if (!correctPassword) {
            throw new InvalidCredentialsException("Invalid username/email or password");
        }

        user.setLastLoginTime(LocalDateTime.now());
        userRepository.save(user);

        return user;
    }
} 