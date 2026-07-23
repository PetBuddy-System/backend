package com.petbuddy.petbuddystore.service;

import com.petbuddy.petbuddystore.dto.response.ProductMediaResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public interface ProductMediaService {

    List<ProductMediaResponse> getProductImages(UUID productId);

    ProductMediaResponse getProductVideo(UUID productId);

    void updateProductImages(UUID productId, List<MultipartFile> images, List<Long> keepImageIds);

    void updateProductVideo(UUID productId, MultipartFile video);
}