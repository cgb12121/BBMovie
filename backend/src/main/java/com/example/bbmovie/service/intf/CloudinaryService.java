package com.example.bbmovie.service.intf;

import java.io.IOException;

import org.springframework.web.multipart.MultipartFile;

public interface CloudinaryService {

     String uploadImage(MultipartFile poster) throws IOException;

     
}
