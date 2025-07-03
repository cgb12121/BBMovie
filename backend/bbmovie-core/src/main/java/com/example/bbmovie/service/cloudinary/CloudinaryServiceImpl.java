package com.example.bbmovie.service.cloudinary;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.example.bbmovie.constant.error.CloudinaryErrorMessages;
import com.example.bbmovie.entity.Movie;
import com.example.bbmovie.exception.MovieNotFoundException;
import com.example.bbmovie.repository.MovieRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Service
@Log4j2
public class CloudinaryServiceImpl implements CloudinaryService {

    private static final String RESOURCE_TYPE_KEY = "resource_type";
    private static final String FOLDER_KEY = "folder";
    private static final String RESOURCE_TYPE_AUTO = "auto";
    private static final String RESOURCE_TYPE_VIDEO = "video";
    private static final String FOLDER_POSTERS = "posters";
    private static final String FOLDER_MOVIES = "movies";
    private static final String FOLDER_TRAILERS = "trailers";

    private final Cloudinary cloudinary;
    private final ObjectMapper objectMapper;
    private final MovieRepository movieRepository;

    public CloudinaryServiceImpl(
            @Qualifier("cloudinaryObjectMapper") ObjectMapper objectMapper,
            MovieRepository movieRepository, Cloudinary cloudinary
    ) {
        this.cloudinary = cloudinary;
        this.objectMapper = objectMapper;
        this.movieRepository = movieRepository;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public CloudinaryResponse uploadImage(Long movieId, MultipartFile file) throws IOException {
        FileValidator.validate(file);

        Movie movie = movieRepository.findById(movieId)
                .orElseThrow(() -> new MovieNotFoundException(
                        String.format(CloudinaryErrorMessages.MOVIE_NOT_FOUND, movieId)
                ));

        Map<?, ?> result = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap(
                RESOURCE_TYPE_KEY, RESOURCE_TYPE_AUTO,
                FOLDER_KEY, FOLDER_POSTERS
        ));

        CloudinaryResponse response = objectMapper.convertValue(result, CloudinaryResponse.class);

        movieRepository.updatePoster(movie.getId(), response.getSecureUrl(), response.getPublicId());

        log.info("Uploaded poster for movie {}: {}", movieId, response.getSecureUrl());
        return response;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public CloudinaryResponse uploadMovie(Long movieId, MultipartFile file) throws IOException {
        FileValidator.validate(file);

        Movie movie = movieRepository.findById(movieId)
                .orElseThrow(() -> new MovieNotFoundException(
                        String.format(CloudinaryErrorMessages.MOVIE_NOT_FOUND, movieId)
                ));

        Map<?, ?> result = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap(
                RESOURCE_TYPE_KEY, RESOURCE_TYPE_VIDEO,
                FOLDER_KEY, FOLDER_MOVIES
        ));

        CloudinaryResponse response = objectMapper.convertValue(result, CloudinaryResponse.class);
        Integer duration = calculateDuration(response);

        movieRepository.updateVideo(movie.getId(), response.getSecureUrl(), response.getPublicId(), duration);

        log.info("Uploaded video for movie {}: {}", movieId, response.getSecureUrl());
        return response;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public CloudinaryResponse uploadTrailer(Long movieId, MultipartFile file) throws IOException {
        FileValidator.validate(file);

        Movie movie = movieRepository.findById(movieId)
                .orElseThrow(() -> new MovieNotFoundException(
                        String.format(CloudinaryErrorMessages.MOVIE_NOT_FOUND, movieId)
                ));

        Map<?, ?> result = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap(
                RESOURCE_TYPE_KEY, RESOURCE_TYPE_AUTO,
                FOLDER_KEY, FOLDER_TRAILERS
        ));

        CloudinaryResponse response = objectMapper.convertValue(result, CloudinaryResponse.class);

        movieRepository.updateTrailer(movie.getId(), response.getSecureUrl(), response.getPublicId());

        log.info("Uploaded trailer for movie {}: {}", movieId, response.getSecureUrl());
        return response;
    }

    @Override
    @Transactional
    public CloudinaryResponse deleteImage(String publicId) throws IOException {
        Movie movie = movieRepository.findByPosterPublicId(publicId)
                .orElseThrow(() -> new MovieNotFoundException(
                        String.format(CloudinaryErrorMessages.MOVIE_NOT_FOUND_BY_PUBLIC_ID, publicId)
                ));

        Map<?, ?> result = cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
        CloudinaryResponse response = objectMapper.convertValue(result, CloudinaryResponse.class);

        movieRepository.deletePoster(movie.getId());

        log.info("Deleted poster for movie {} with public ID: {}", movie.getId(), publicId);
        return response;
    }

    @Override
    @Transactional
    public CloudinaryResponse deleteMovie(String publicId) throws IOException {
        Movie movie = movieRepository.findByVideoPublicId(publicId)
                .orElseThrow(() -> new MovieNotFoundException(
                        String.format(CloudinaryErrorMessages.MOVIE_NOT_FOUND_BY_PUBLIC_ID, publicId)
                ));

        Map<?, ?> result = cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
        CloudinaryResponse response = objectMapper.convertValue(result, CloudinaryResponse.class);

        movieRepository.deleteVideo(movie.getId());

        log.info("Deleted video for movie {} with public ID: {}", movie.getId(), publicId);
        return response;
    }

    @Override
    @Transactional
    public CloudinaryResponse deleteTrailer(String publicId) throws IOException {
        Movie movie = movieRepository.findByTrailerPublicId(publicId)
                .orElseThrow(() -> new MovieNotFoundException(
                        String.format(CloudinaryErrorMessages.MOVIE_NOT_FOUND_BY_PUBLIC_ID, publicId)
                ));

        Map<?, ?> result = cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
        CloudinaryResponse response = objectMapper.convertValue(result, CloudinaryResponse.class);

        movieRepository.deleteTrailer(movie.getId());

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