package com.bbmovie.mediauploadservice.repository;

import com.bbmovie.mediauploadservice.entity.MediaVariant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface MediaVariantRepository extends JpaRepository<MediaVariant, UUID> {
}
