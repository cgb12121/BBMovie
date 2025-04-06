package com.example.bbmovie.repository;

import com.example.bbmovie.model.base.BaseEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

import java.util.List;
import java.util.Optional;

@NoRepositoryBean
public interface BaseRepository<T extends BaseEntity> extends JpaRepository<T, Long> {
    Optional<T> findByIdAndIsActiveTrue(Long id);
    List<T> findAllByIsActiveTrue();
} 