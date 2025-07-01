package com.example.bbmovie.repository;

import com.example.bbmovie.entity.Movie;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface MovieRepository extends BaseRepository<Movie> {
    @Modifying
    @Query("UPDATE Movie m SET m.posterUrl = :posterUrl, m.posterPublicId = :posterPublicId WHERE m.id = :movieId")
    void updatePoster(Long movieId, String posterUrl, String posterPublicId);

    @Modifying
    @Query("UPDATE Movie m SET m.videoUrl = :url, m.videoPublicId = :publicId, m.durationMinutes = :duration WHERE m.id = :id")
    void updateVideo(Long id, String url, String publicId, Integer duration);

    @Modifying
    @Query("UPDATE Movie m SET m.trailerUrl = :url, m.trailerPublicId = :publicId WHERE m.id = :id")
    void updateTrailer(Long id, String url, String publicId);

    @Modifying
    @Query("UPDATE Movie m SET m.posterUrl = NULL, m.posterPublicId = NULL WHERE m.id = :id")
    void deletePoster(Long id);

    @Modifying
    @Query("UPDATE Movie m SET m.videoUrl = NULL, m.videoPublicId = NULL, m.durationMinutes = NULL WHERE m.id = :id")
    void deleteVideo(Long id);

    @Modifying
    @Query("UPDATE Movie m SET m.trailerUrl = NULL, m.trailerPublicId = NULL WHERE m.id = :id")
    void deleteTrailer(Long id);

    Page<Movie> findAllByIsActiveTrue(Pageable pageable);
    
    @Query("SELECT m FROM Movie m JOIN m.genres g WHERE g.id = :genreId AND m.isActive = true")
    Page<Movie> findByGenreId(@Param("genreId") Long genreId, Pageable pageable);
    
    @Query("SELECT m FROM Movie m WHERE m.releaseDate BETWEEN :startDate AND :endDate AND m.isActive = true")
    Page<Movie> findByReleaseDateBetween(@Param("startDate") LocalDate startDate, 
                                        @Param("endDate") LocalDate endDate, 
                                        Pageable pageable);
    
    @Query("SELECT m FROM Movie m WHERE LOWER(m.title) LIKE LOWER(CONCAT('%', :title, '%')) AND m.isActive = true")
    Page<Movie> findByTitleContainingIgnoreCase(@Param("title") String title, Pageable pageable);

    Optional<Movie> findByPosterPublicId(String publicId);

    Optional<Movie> findByVideoPublicId(String publicId);

    Optional<Movie> findByTrailerPublicId(String publicId);

    Optional<Movie> findByTitle(String name);
}