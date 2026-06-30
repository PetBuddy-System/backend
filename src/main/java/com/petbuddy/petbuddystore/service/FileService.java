package com.petbuddy.petbuddystore.service;

import com.petbuddy.petbuddystore.model.MediaFile;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface FileService {
    MediaFile uploadProductImage(MultipartFile file);
    MediaFile uploadPetImage(MultipartFile file);
    MediaFile uploadBlogImage(MultipartFile file);
    MediaFile uploadProductImageFromBytes(byte[] bytes);

}