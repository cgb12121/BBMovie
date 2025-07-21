package com.bbmovie.auth.repository;

import com.bbmovie.auth.entity.jose.JwkKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JwkKeyRepository extends JpaRepository<JwkKey, String> {

}
