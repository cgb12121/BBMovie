package com.example.bbmovie.repository;

import com.example.bbmovie.entity.Genre;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GenreRepository extends BaseRepository<Genre> {
    Optional<Genre> findByName(String name);
}