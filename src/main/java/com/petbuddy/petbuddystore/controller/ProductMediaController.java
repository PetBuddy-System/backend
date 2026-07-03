package com.petbuddy.petbuddystore.controller;

import com.petbuddy.petbuddystore.common.response.ApiResponse;
import com.petbuddy.petbuddystore.dto.response.ProductMediaResponse;
import com.petbuddy.petbuddystore.service.ProductMediaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Product Media API", description = "Quản lý media (ảnh/video) của sản phẩm")
public class ProductMediaController {

    ProductMediaService productMediaService;

    @GetMapping("/{productId}/images")
    @Operation(summary = "Get all images of product",
            description = "Lấy danh sách tất cả ảnh của sản phẩm (tối đa 4 ảnh)"
    )
    public ResponseEntity<ApiResponse<List<ProductMediaResponse>>> getProductImages(
            @PathVariable UUID productId
    ) {
        return ResponseEntity.ok(ApiResponse.success(productMediaService.getProductImages(productId)));
    }

    @GetMapping("/{productId}/video")
    @Operation(summary = "Get product video",
            description = "Lấy video của sản phẩm (chỉ 1 video duy nhất)"
    )
    public ResponseEntity<ApiResponse<ProductMediaResponse>> getProductVideo(
            @PathVariable UUID productId
    ) {
        return ResponseEntity.ok(ApiResponse.success(productMediaService.getProductVideo(productId)));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    @PutMapping(value = "/{productId}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Update product images",
            description = "Cập nhật ảnh của sản phẩm. " +
                    "- images: danh sách ảnh mới (null: giữ nguyên, []: xóa hết)\n" +
                    "- keepImageIds: danh sách ID ảnh cũ muốn giữ lại"
    )
    public ResponseEntity<ApiResponse<Void>> updateProductImages(
            @PathVariable UUID productId,
            @RequestPart(value = "images", required = false) List<MultipartFile> images,
            @RequestParam(value = "keepImageIds", required = false) List<Long> keepImageIds
    ) {
        productMediaService.updateProductImages(productId, images, keepImageIds);
        return ResponseEntity.ok(ApiResponse.success("Images updated successfully", null));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    @PutMapping(value = "/{productId}/video", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Update product video",
            description = "Cập nhật video của sản phẩm. " +
                    "- video = null: giữ nguyên video cũ\n" +
                    "- video = empty: xóa video\n" +
                    "- video có file: thay thế video cũ"
    )
    public ResponseEntity<ApiResponse<Void>> updateProductVideo(
            @PathVariable UUID productId,
            @RequestPart(value = "video", required = false) MultipartFile video
    ) {
        productMediaService.updateProductVideo(productId, video);
        return ResponseEntity.ok(ApiResponse.success("Video updated successfully", null));
    }
}