package com.petbuddy.petbuddystore.controller;

import com.petbuddy.petbuddystore.common.enums.ReviewStatus;
import com.petbuddy.petbuddystore.common.response.ApiResponse;
import com.petbuddy.petbuddystore.dto.request.ProductReviewCreationRequest;
import com.petbuddy.petbuddystore.dto.request.ProductReviewUpdateRequest;
import com.petbuddy.petbuddystore.dto.request.ReviewStatusUpdateRequest;
import com.petbuddy.petbuddystore.dto.response.ProductReviewManagerResponse;
import com.petbuddy.petbuddystore.dto.response.ProductReviewResponse;
import com.petbuddy.petbuddystore.service.ProductReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Product Review API", description = "Quản lí đánh giá sản phẩm")
public class ProductReviewController {

    ProductReviewService reviewService;

    // ============ PUBLIC ============
    @GetMapping("/products/{productId}/reviews")
    @Operation(description = "Lấy danh sách đánh giá của sản phẩm (có lọc theo số sao)")
    public ResponseEntity<ApiResponse<Page<ProductReviewResponse>>> getProductReviews(
            @PathVariable String productId,
            @RequestParam(required = false) Integer rating,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection
    ) {
        Sort sort = Sort.by(Sort.Direction.fromString(sortDirection), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success(reviewService.getProductReviews(productId, rating, pageable)));
    }

    // ============ USER ============
    @PostMapping("/products/{productId}/reviews")
    @Operation(description = "Tạo đánh giá sản phẩm")
    public ResponseEntity<ApiResponse<ProductReviewResponse>> createReview(
            @PathVariable String productId,
            @Valid @RequestBody ProductReviewCreationRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Review created successfully", reviewService.createReview(productId, request)));
    }

    @GetMapping("/products/{productId}/reviews/me")
    @Operation(description = "Lấy đánh giá của tôi cho sản phẩm (trả về null nếu chưa review)")
    public ResponseEntity<ApiResponse<ProductReviewResponse>> getMyReview(
            @PathVariable String productId
    ) {
        ProductReviewResponse response = reviewService.getMyReview(productId);
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success(response));
    }

    @PutMapping("/reviews/{reviewId}")
    @Operation(description = "Cập nhật đánh giá của tôi")
    public ResponseEntity<ApiResponse<ProductReviewResponse>> updateReview(
            @PathVariable String reviewId,
            @Valid @RequestBody ProductReviewUpdateRequest request
    ) {
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success("Review updated successfully", reviewService.updateReview(reviewId, request)));
    }

    @DeleteMapping("/reviews/{reviewId}")
    @Operation(description = "Xóa đánh giá của tôi")
    public ResponseEntity<ApiResponse<Void>> deleteReview(
            @PathVariable String reviewId
    ) {
        reviewService.deleteReview(reviewId);
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success("Review deleted successfully", null));
    }

    // ============ MANAGER/ADMIN ============
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @GetMapping("/management/reviews")
    @Operation(description = "Quản lí danh sách đánh giá (tìm kiếm, lọc theo rating và status, sắp xếp)")
    public ResponseEntity<ApiResponse<Page<ProductReviewManagerResponse>>> getAllReviews(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer rating,
            @RequestParam(required = false) ReviewStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection
    ) {
        Sort sort = Sort.by(Sort.Direction.fromString(sortDirection), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success(reviewService.getAllReviews(keyword, rating, status, pageable)));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @PatchMapping("/management/reviews/{reviewId}/status")
    @Operation(description = "Cập nhật trạng thái đánh giá")
    public ResponseEntity<ApiResponse<Void>> updateReviewStatus(
            @PathVariable String reviewId,
            @Valid @RequestBody ReviewStatusUpdateRequest request
    ) {
        reviewService.updateReviewStatus(reviewId, request);
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success("Review status updated successfully", null));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @GetMapping("/management/reviews/{reviewId}")
    @Operation(description = "Lấy chi tiết đánh giá (có avatar đầy đủ)")
    public ResponseEntity<ApiResponse<ProductReviewManagerResponse>> getReviewDetailForManager(
            @PathVariable String reviewId
    ) {
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success(reviewService.getReviewDetailForManager(reviewId)));
    }
}