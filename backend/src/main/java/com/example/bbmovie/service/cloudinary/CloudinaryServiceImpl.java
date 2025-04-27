package com.example.bbmovie.service.cloudinary;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.example.bbmovie.constant.ErrorMessages;
import com.example.bbmovie.entity.Movie;
import com.example.bbmovie.exception.MovieNotFoundException;
import com.example.bbmovie.repository.MovieRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Service
@Log4j2
public class CloudinaryServiceImpl implements CloudinaryService {


    private final Cloudinary cloudinary;
    private final ObjectMapper objectMapper;
    private final MovieRepository movieRepository;

    public CloudinaryServiceImpl(
            Cloudinary cloudinary,
            @Qualifier("cloudinaryObjectMapper") ObjectMapper objectMapper,
            MovieRepository movieRepository
    ) {
        this.cloudinary = cloudinary;
        this.objectMapper = objectMapper;
        this.movieRepository = movieRepository;
    }

    @Override
    @Transactional
    public CloudinaryResponse uploadImage(Long movieId, MultipartFile file) throws IOException {
        FileValidator.validate(file);

        Movie movie = movieRepository.findById(movieId)
                .orElseThrow(() -> new MovieNotFoundException(
                        String.format(ErrorMessages.MOVIE_NOT_FOUND, movieId)
                ));

        Map<?, ?> result = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap(
                "resource_type", "auto",
                "folder", "posters"
        ));

        CloudinaryResponse response = objectMapper.convertValue(result, CloudinaryResponse.class);

        movie.setPosterUrl(response.getSecureUrl());
        movie.setPosterPublicId(response.getPublicId());
        movieRepository.save(movie);

        log.info("Uploaded poster for movie {}: {}", movieId, response.getSecureUrl());
        return response;
    }

    @Override
    @Transactional
    public CloudinaryResponse uploadMovie(Long movieId, MultipartFile file) throws IOException {
        FileValidator.validate(file);

        Movie movie = movieRepository.findById(movieId)
                .orElseThrow(() -> new MovieNotFoundException(
                        String.format(ErrorMessages.MOVIE_NOT_FOUND, movieId)
                ));

        Map<?, ?> result = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap(
                "resource_type", "video",
                "folder", "movies"
        ));

        CloudinaryResponse response = objectMapper.convertValue(result, CloudinaryResponse.class);

        movie.setVideoUrl(response.getSecureUrl());
        movie.setVideoPublicId(response.getPublicId());
        movie.setDurationMinutes(calculateDuration(response));
        movieRepository.save(movie);

        log.info("Uploaded video for movie {}: {}", movieId, response.getSecureUrl());
        return response;
    }

    @Override
    @Transactional
    public CloudinaryResponse uploadTrailer(Long movieId, MultipartFile file) throws IOException {
        FileValidator.validate(file);

        Movie movie = movieRepository.findById(movieId)
                .orElseThrow(() -> new MovieNotFoundException(
                        String.format(ErrorMessages.MOVIE_NOT_FOUND, movieId)
                ));

        Map<?, ?> result = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap(
                "resource_type", "auto",
                "folder", "trailers"
        ));

        CloudinaryResponse response = objectMapper.convertValue(result, CloudinaryResponse.class);

        movie.setTrailerUrl(response.getSecureUrl());
        movie.setTrailerPublicId(response.getPublicId());
        movieRepository.save(movie);

        log.info("Uploaded trailer for movie {}: {}", movieId, response.getSecureUrl());
        return response;
    }

    @Override
    @Transactional
    public CloudinaryResponse deleteImage(String publicId) throws IOException {
        Movie movie = movieRepository.findByPosterPublicId(publicId)
                .orElseThrow(() -> new MovieNotFoundException(
                        String.format(ErrorMessages.MOVIE_NOT_FOUND_BY_PUBLIC_ID, publicId)
                ));

        Map<?, ?> result = cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
        CloudinaryResponse response = objectMapper.convertValue(result, CloudinaryResponse.class);

        movie.setPosterUrl(null);
        movie.setPosterPublicId(null);
        movieRepository.save(movie);

        log.info("Deleted poster for movie {} with public ID: {}", movie.getId(), publicId);
        return response;
    }

    @Override
    @Transactional
    public CloudinaryResponse deleteMovie(String publicId) throws IOException {
        Movie movie = movieRepository.findByVideoPublicId(publicId)
                .orElseThrow(() -> new MovieNotFoundException(
                        String.format(ErrorMessages.MOVIE_NOT_FOUND_BY_PUBLIC_ID, publicId)
                ));

        Map<?, ?> result = cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
        CloudinaryResponse response = objectMapper.convertValue(result, CloudinaryResponse.class);

        movie.setVideoUrl(null);
        movie.setVideoPublicId(null);
        movie.setDurationMinutes(null);
        movieRepository.save(movie);

        log.info("Deleted video for movie {} with public ID: {}", movie.getId(), publicId);
        return response;
    }

    @Override
    @Transactional
    public CloudinaryResponse deleteTrailer(String publicId) throws IOException {
        Movie movie = movieRepository.findByTrailerPublicId(publicId)
                .orElseThrow(() -> new MovieNotFoundException(
                        String.format(ErrorMessages.MOVIE_NOT_FOUND_BY_PUBLIC_ID, publicId)
                ));

        Map<?, ?> result = cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
        CloudinaryResponse response = objectMapper.convertValue(result, CloudinaryResponse.class);

        movie.setTrailerUrl(null);
        movie.setTrailerPublicId(null);
        movieRepository.save(movie);

        log.info("Deleted trailer for movie {} with public ID: {}", movie.getId(), publicId);
        return response;
    }

    private Integer calculateDuration(CloudinaryResponse response) {
        Integer duration = response.getDuration();
        if (duration != null && duration > 0) {
            return duration / 60;
        }
        return 0;
    }
}
