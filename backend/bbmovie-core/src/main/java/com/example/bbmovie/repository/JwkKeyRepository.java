package com.example.bbmovie.repository;

import com.example.bbmovie.entity.jose.JwkKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JwkKeyRepository extends JpaRepository<JwkKey, String> {

}
