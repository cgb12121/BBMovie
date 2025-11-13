package com.bbmovie.auth.repository;

import com.bbmovie.auth.entity.jose.JoseKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface JwkKeyRepository extends JpaRepository<JoseKey, UUID> {

}
