package com.petbuddy.petbuddystore.service;

import com.petbuddy.petbuddystore.dto.request.ProductImportRequest;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface FileService {

    List<String> uploadProductImages(List<MultipartFile> files);

    String uploadPetImage(MultipartFile file);

    String uploadBlogImage(MultipartFile file);


}