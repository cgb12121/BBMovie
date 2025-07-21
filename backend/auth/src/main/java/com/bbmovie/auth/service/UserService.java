package com.bbmovie.auth.service;

import com.bbmovie.auth.entity.User;
import com.bbmovie.auth.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;

    @Autowired
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public User createUserFromOAuth2(User user) {
        userRepository.save(user);
        return user;
    }
}
