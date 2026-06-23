package com.petbuddy.petbuddystore.service;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface FileService {

    String uploadProductImage(MultipartFile file);

    String uploadPetImage(MultipartFile file);

    String uploadBlogImage(MultipartFile file);


}