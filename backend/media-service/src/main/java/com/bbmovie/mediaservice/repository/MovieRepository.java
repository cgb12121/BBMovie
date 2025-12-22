package com.bbmovie.mediaservice.repository;

import com.bbmovie.mediaservice.entity.Movie;
import com.bbmovie.mediaservice.entity.MovieStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MovieRepository extends JpaRepository<Movie, UUID> {
    
    Optional<Movie> findByMovieId(UUID movieId);
    
    List<Movie> findByStatus(MovieStatus status);
    
    @Query("SELECT m FROM Movie m WHERE m.fileId = :fileId")
    Optional<Movie> findByFileId(@Param("fileId") String fileId);
    
    List<Movie> findByTitleContainingIgnoreCase(String title);
}