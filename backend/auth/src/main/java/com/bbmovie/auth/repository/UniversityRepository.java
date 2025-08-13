package com.bbmovie.auth.repository;

import com.bbmovie.auth.entity.University;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UniversityRepository extends JpaRepository<University, Long> {
    Optional<University> findByNameContainingIgnoreCase(String name);
}
