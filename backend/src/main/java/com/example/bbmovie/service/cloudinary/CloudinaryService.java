package com.example.bbmovie.service.cloudinary;

import java.io.IOException;

import org.springframework.web.multipart.MultipartFile;

public interface CloudinaryService {
    CloudinaryResponse uploadImage(Long movieId, MultipartFile file) throws IOException;

    CloudinaryResponse uploadMovie(Long movieId, MultipartFile file) throws IOException;

    CloudinaryResponse uploadTrailer(Long movieId, MultipartFile file) throws IOException;

    CloudinaryResponse deleteImage(String publicId) throws IOException;

    CloudinaryResponse deleteMovie(String publicId) throws IOException;

    CloudinaryResponse deleteTrailer(String publicId) throws IOException;
}
