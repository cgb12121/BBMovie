package com.bbmovie.auth.repository;

import com.bbmovie.auth.entity.University;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UniversityRepository extends JpaRepository<University, Long> {
    Optional<University> findByNameContainingIgnoreCase(String name);

    Optional<University> findByDomainsContainingIgnoreCase(String domain);

    Page<University> findByCountryIgnoreCase(String trim, Pageable pageable);

    Page<University> findByAlphaTwoCodeIgnoreCase(String trim, Pageable pageable);
}
