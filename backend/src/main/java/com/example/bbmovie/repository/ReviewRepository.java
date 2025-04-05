package com.example.bbmovie.repository;

import com.example.bbmovie.model.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ReviewRepository extends BaseRepository<Review> {
    Page<Review> findByMovieIdAndIsPublicTrue(Long movieId, Pageable pageable);
    
    Page<Review> findByUserId(Long userId, Pageable pageable);
    
    @Query("SELECT r FROM Review r WHERE r.movie.id = :movieId AND r.isApproved = true AND r.isPublic = true")
    Page<Review> findApprovedReviewsByMovieId(@Param("movieId") Long movieId, Pageable pageable);
    
    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.movie.id = :movieId AND r.isApproved = true")
    Double calculateAverageRatingByMovieId(@Param("movieId") Long movieId);
} 