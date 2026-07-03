package com.petbuddy.petbuddystore.service;

import com.petbuddy.petbuddystore.dto.response.ProductMediaResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public interface ProductMediaService {

    /**
     * Lấy danh sách tất cả ảnh của sản phẩm
     */
    List<ProductMediaResponse> getProductImages(UUID productId);

    /**
     * Lấy video của sản phẩm
     */
    ProductMediaResponse getProductVideo(UUID productId);

    /**
     * Cập nhật ảnh của sản phẩm
     * @param productId ID sản phẩm
     * @param images Danh sách ảnh mới (null: giữ nguyên, []: xóa hết)
     * @param keepImageIds Danh sách ID ảnh cũ muốn giữ lại
     */
    void updateProductImages(UUID productId, List<MultipartFile> images, List<Long> keepImageIds);

    /**
     * Cập nhật video của sản phẩm
     * @param productId ID sản phẩm
     * @param video Video mới (null: giữ nguyên, empty: xóa, có file: thay thế)
     */
    void updateProductVideo(UUID productId, MultipartFile video);
}