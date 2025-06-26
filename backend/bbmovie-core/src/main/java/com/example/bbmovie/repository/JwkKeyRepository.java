package com.example.bbmovie.repository;

import com.example.bbmovie.entity.jose.JwkKey;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JwkKeyRepository extends JpaRepository<JwkKey, String> {
    List<JwkKey> findAllByActiveTrue();
}
