package com.example.bbmovie.service;

import com.example.bbmovie.dto.response.UserProfileResponse;
import com.example.bbmovie.entity.User;
import com.example.bbmovie.exception.UserNotFoundException;
import com.example.bbmovie.repository.UserRepository;
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

    public UserProfileResponse findById(String id) {
        Optional<User> user = userRepository.findById(Long.valueOf(id));
        return user.map(value -> UserProfileResponse.builder()
                .displayedUsername(value.getDisplayedUsername())
                .profilePictureUrl(value.getProfilePictureUrl())
                .build()
        ).orElseThrow(() -> new UserNotFoundException("Could not find user with id: " + id));
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public User createUserFromOAuth2(User user) {
        userRepository.save(user);
        return user;
    }

    public boolean isOwner(String userName, Long accountId) {
        return userName.equalsIgnoreCase("quanbaoyb@gmail.com") && accountId == 1L ;
    }
}
