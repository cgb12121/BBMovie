package com.example.bbmovie.repository;

import com.example.bbmovie.model.Movie;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface MovieRepository extends BaseRepository<Movie> {
    Page<Movie> findAllByIsActiveTrue(Pageable pageable);
    
    @Query("SELECT m FROM Movie m JOIN m.genres g WHERE g.id = :genreId AND m.isActive = true")
    Page<Movie> findByGenreId(@Param("genreId") Long genreId, Pageable pageable);
    
    @Query("SELECT m FROM Movie m WHERE m.releaseDate BETWEEN :startDate AND :endDate AND m.isActive = true")
    Page<Movie> findByReleaseDateBetween(@Param("startDate") LocalDate startDate, 
                                        @Param("endDate") LocalDate endDate, 
                                        Pageable pageable);
    
    @Query("SELECT m FROM Movie m WHERE LOWER(m.title) LIKE LOWER(CONCAT('%', :title, '%')) AND m.isActive = true")
    Page<Movie> findByTitleContainingIgnoreCase(@Param("title") String title, Pageable pageable);
    
    List<Movie> findByTmdbId(String tmdbId);
} 