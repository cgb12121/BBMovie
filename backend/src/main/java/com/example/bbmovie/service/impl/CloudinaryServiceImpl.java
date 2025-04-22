package com.example.bbmovie.service.impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.example.bbmovie.service.intf.CloudinaryService;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CloudinaryServiceImpl implements CloudinaryService {
    
    private final Cloudinary cloudinary;
    
    public String uploadImage(MultipartFile file) throws IOException {
        Map<?, ?> result = cloudinary.uploader()
            .upload(file.getBytes(), ObjectUtils.asMap(
                "resource_type", "auto",
                "folder", "posters"
            ));
        return (String) result.get("secure_url");
    }

    public String uploadMovie(MultipartFile file) throws IOException {
        Map<?, ?> result = cloudinary.uploader()
            .upload(file.getBytes(), ObjectUtils.asMap(
                "resource_type", "auto",
                "folder", "movies"
            ));
        return (String) result.get("secure_url");
    }

    public String uploadTrailer(MultipartFile file) throws IOException {
        Map<?, ?> result = cloudinary.uploader()
            .upload(file.getBytes(), ObjectUtils.asMap(
                "resource_type", "auto",
                "folder", "trailers"
            ));
        return (String) result.get("secure_url");
    }
}
