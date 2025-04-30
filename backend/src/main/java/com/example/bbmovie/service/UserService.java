package com.example.bbmovie.service;

import com.example.bbmovie.entity.User;
import com.example.bbmovie.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public void createUserFromOAuth2(User user) {
        userRepository.save(user);
    }
}
