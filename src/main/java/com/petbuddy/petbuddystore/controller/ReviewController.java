package com.petbuddy.petbuddystore.controller;

import com.petbuddy.petbuddystore.common.enums.ReviewStatus;
import com.petbuddy.petbuddystore.common.response.ApiResponse;
import com.petbuddy.petbuddystore.dto.request.OrderReviewRequest;
import com.petbuddy.petbuddystore.dto.request.ProductReviewCreationRequest;
import com.petbuddy.petbuddystore.dto.request.ProductReviewUpdateRequest;
import com.petbuddy.petbuddystore.dto.request.ReviewStatusUpdateRequest;
import com.petbuddy.petbuddystore.dto.response.OrderReviewResponse;
import com.petbuddy.petbuddystore.dto.response.ProductReviewManagerResponse;
import com.petbuddy.petbuddystore.dto.response.ProductReviewResponse;
import com.petbuddy.petbuddystore.service.ReviewService;
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
@Tag(name = "Review API", description = "Quản lý đánh giá")
public class ReviewController {

    ReviewService reviewService;

    // ========================================
    // PRODUCT REVIEW APIs (GIỮ NGUYÊN DTO CŨ)
    // ========================================

    @GetMapping("/products/{productId}/reviews")
    @Operation(description = "Lấy danh sách đánh giá của sản phẩm")
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
        return ResponseEntity.ok(
                ApiResponse.success(reviewService.getProductReviews(productId, rating, pageable))
        );
    }

    @PostMapping("/products/{productId}/reviews")
    @Operation(description = "Tạo đánh giá sản phẩm")
    public ResponseEntity<ApiResponse<ProductReviewResponse>> createProductReview(
            @PathVariable String productId,
            @Valid @RequestBody ProductReviewCreationRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        "Review created successfully",
                        reviewService.createProductReview(productId, request)
                ));
    }

    @GetMapping("/products/{productId}/reviews/me")
    @Operation(description = "Lấy đánh giá của tôi cho sản phẩm")
    public ResponseEntity<ApiResponse<ProductReviewResponse>> getMyProductReview(
            @PathVariable String productId
    ) {
        return ResponseEntity.ok(
                ApiResponse.success(reviewService.getMyProductReview(productId))
        );
    }

    @PutMapping("/reviews/{reviewId}")
    @Operation(description = "Cập nhật đánh giá của tôi")
    public ResponseEntity<ApiResponse<ProductReviewResponse>> updateReview(
            @PathVariable String reviewId,
            @Valid @RequestBody ProductReviewUpdateRequest request
    ) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        "Review updated successfully",
                        reviewService.updateReview(reviewId, request)
                )
        );
    }

    @DeleteMapping("/reviews/{reviewId}")
    @Operation(description = "Xóa đánh giá của tôi")
    public ResponseEntity<ApiResponse<Void>> deleteReview(
            @PathVariable String reviewId
    ) {
        reviewService.deleteReview(reviewId);
        return ResponseEntity.ok(
                ApiResponse.success("Review deleted successfully", null)
        );
    }

    // ========================================
    // ORDER REVIEW APIs (THÊM MỚI)
    // ========================================

    @PostMapping("/orders/{orderId}/reviews")
    @Operation(description = "Tạo đánh giá đơn hàng")
    public ResponseEntity<ApiResponse<OrderReviewResponse>> createOrderReview(
            @PathVariable Long orderId,
            @Valid @RequestBody OrderReviewRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        "Order review created successfully",
                        reviewService.createOrderReview(orderId, request)
                ));
    }

    @GetMapping("/orders/{orderId}/reviews")
    @Operation(description = "Lấy đánh giá của đơn hàng")
    public ResponseEntity<ApiResponse<OrderReviewResponse>> getOrderReview(
            @PathVariable Long orderId
    ) {
        return ResponseEntity.ok(
                ApiResponse.success(reviewService.getOrderReview(orderId))
        );
    }

    // ========================================
    // MANAGEMENT APIs (GIỮ NGUYÊN)
    // ========================================

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @GetMapping("/management/reviews")
    @Operation(description = "Quản lý danh sách đánh giá")
    public ResponseEntity<ApiResponse<Page<ProductReviewManagerResponse>>> getAllReviews(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer rating,
            @RequestParam(required = false) ReviewStatus status,
            @RequestParam(required = false) String reviewType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection
    ) {
        Sort sort = Sort.by(Sort.Direction.fromString(sortDirection), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);
        return ResponseEntity.ok(
                ApiResponse.success(reviewService.getAllReviews(keyword, rating, status, reviewType, pageable))
        );
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @PatchMapping("/management/reviews/{reviewId}/status")
    @Operation(description = "Cập nhật trạng thái đánh giá")
    public ResponseEntity<ApiResponse<Void>> updateReviewStatus(
            @PathVariable String reviewId,
            @Valid @RequestBody ReviewStatusUpdateRequest request
    ) {
        reviewService.updateReviewStatus(reviewId, request);
        return ResponseEntity.ok(
                ApiResponse.success("Review status updated successfully", null)
        );
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @GetMapping("/management/reviews/{reviewId}")
    @Operation(description = "Lấy chi tiết đánh giá cho quản lý")
    public ResponseEntity<ApiResponse<ProductReviewManagerResponse>> getReviewDetailForManager(
            @PathVariable String reviewId
    ) {
        return ResponseEntity.ok(
                ApiResponse.success(reviewService.getReviewDetailForManager(reviewId))
        );
    }
}