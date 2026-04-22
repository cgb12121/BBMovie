package com.bbmovie.camundaengine.repository;

import com.bbmovie.camundaengine.entity.University;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UniversityRepository extends JpaRepository<University, UUID> {
    Page<University> findByCountryIgnoreCase(String country, Pageable pageable);
    Page<University> findByAlphaTwoCodeIgnoreCase(String code, Pageable pageable);
    Optional<University> findByDomainsContainingIgnoreCase(String domain);
    Optional<University> findByNameContainingIgnoreCase(String name);
}
