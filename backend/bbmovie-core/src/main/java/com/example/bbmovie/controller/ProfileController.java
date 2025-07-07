package com.example.bbmovie.controller;

import com.example.bbmovie.dto.ApiResponse;
import com.example.bbmovie.dto.response.UserProfileResponse;
import com.example.bbmovie.entity.User;
import com.example.bbmovie.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Objects;
import java.util.Optional;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    private final UserService userService;

    @Autowired
    public ProfileController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getUserProfile(
            @PathVariable String id, @AuthenticationPrincipal UserDetails userDetails
    ) {
        UserProfileResponse response = userService.findById(id);
        Optional<User> currentUser = userService.findByEmail(userDetails.getUsername());
        if (currentUser.isPresent() && Objects.equals(String.valueOf(id), String.valueOf(currentUser.get().getId()))) {
            return ResponseEntity.ok(ApiResponse.success(response, "This is self profile and should it contain more information?"));
        }
        return ResponseEntity.ok(ApiResponse.success(response, "This is other user's profile"));
    }
}
