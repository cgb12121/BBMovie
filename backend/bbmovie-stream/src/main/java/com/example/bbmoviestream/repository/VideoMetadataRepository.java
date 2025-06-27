package com.example.bbmoviestream.repository;

import com.example.bbmoviestream.entity.VideoMetadata;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VideoMetadataRepository extends JpaRepository<VideoMetadata, Long> {
}
