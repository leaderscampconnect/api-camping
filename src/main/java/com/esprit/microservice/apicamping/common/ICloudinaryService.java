package com.esprit.microservice.apicamping.common;

import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

public interface ICloudinaryService {
    Map<String, String> uploadImage(MultipartFile file);
    void deleteImage(String publicId);
}
