package com.petbuddy.petbuddystore.controller;

import com.petbuddy.petbuddystore.common.enums.ProductStatus;
import com.petbuddy.petbuddystore.common.response.ApiResponse;
import com.petbuddy.petbuddystore.dto.request.ProductCreationRequest;
import com.petbuddy.petbuddystore.dto.request.ProductUpdateRequest;
import com.petbuddy.petbuddystore.dto.response.ProductDetailResponse;
import com.petbuddy.petbuddystore.dto.response.ProductManagementResponse;
import com.petbuddy.petbuddystore.dto.response.ProductPublicResponse;
import com.petbuddy.petbuddystore.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
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
@Tag(name = "Product API", description = "Quản lý thông tin sản phẩm")
public class ProductController {

    ProductService productService;
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Create product",
            description = "Tạo mới thông tin sản phẩm. API này chỉ tạo phần Product, không xử lý lô hàng. Lô hàng sẽ được xử lý ở ProductBatch API.")
    public ResponseEntity<ApiResponse<ProductManagementResponse>> createProduct(
            @Valid @ModelAttribute ProductCreationRequest request,
            @RequestPart(value = "images", required = false) List<MultipartFile> images
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        "Product created successfully",
                        productService.createProduct(request, images)
                ));
    }

    @GetMapping
    @Operation(summary = "Get products for user",
            description = "Lấy danh sách sản phẩm cho UI người dùng. Chỉ hiển thị sản phẩm ACTIVE, hỗ trợ phân trang, tìm kiếm, lọc category, brandName và sort."
    )
    public ResponseEntity<ApiResponse<Page<ProductPublicResponse>>> getProductsForUser(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String brandName,
            @RequestParam(defaultValue = "date_desc") String sortBy,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(ApiResponse.success(productService.getProductsForUser(keyword, categoryId, brandName, sortBy, pageable)));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'STAFF')")
    @GetMapping("/management")
    @Operation(summary = "Get products for management",
            description = "Lấy danh sách sản phẩm cho ADMIN/MANAGER/STAFF. API này có thể hiển thị cả ACTIVE, INACTIVE và DELETED, đồng thời có nhiều field quản lý hơn API user."
    )
    public ResponseEntity<ApiResponse<Page<ProductManagementResponse>>> getProductsForManagement(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String brandName,
            @RequestParam(required = false) ProductStatus status,
            @RequestParam(defaultValue = "date_desc") String sortBy,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(ApiResponse.success(productService.getProductsForManagement(keyword, categoryId, brandName, status, sortBy, pageable)));}

    @GetMapping("/{productId}")
    @Operation(summary = "Get product detail",
            description = "Lấy chi tiết sản phẩm theo id. Dùng cho màn hình chi tiết sản phẩm hoặc form chỉnh sửa."
    )
    public ResponseEntity<ApiResponse<ProductDetailResponse>> getProductDetail(@PathVariable UUID productId) {
        return ResponseEntity.ok( ApiResponse.success(productService.getProductDetail(productId)));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @PatchMapping(value = "/{productId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Update product", description = "Cập nhật thông tin sản phẩm.")
    public ResponseEntity<ApiResponse<ProductManagementResponse>> updateProduct(
            @PathVariable UUID productId,
            @Valid @ModelAttribute ProductUpdateRequest request,
            @RequestPart(value = "images", required = false) List<MultipartFile> images
    ) {
        return ResponseEntity.ok(ApiResponse.success("Product updated successfully", productService.updateProduct(productId, request, images)));
    }
}