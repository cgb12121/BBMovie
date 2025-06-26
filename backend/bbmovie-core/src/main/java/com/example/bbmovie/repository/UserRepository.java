package com.example.bbmovie.repository;

import com.example.bbmovie.entity.User;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends BaseRepository<User> {
    Optional<User> findByDisplayedUsername(String displayedUsername);
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
} 