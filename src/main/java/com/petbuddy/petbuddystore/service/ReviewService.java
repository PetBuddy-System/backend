package com.petbuddy.petbuddystore.service;

import com.petbuddy.petbuddystore.common.enums.ReviewStatus;
import com.petbuddy.petbuddystore.dto.request.OrderReviewRequest;
import com.petbuddy.petbuddystore.dto.request.ProductReviewCreationRequest;
import com.petbuddy.petbuddystore.dto.request.ProductReviewUpdateRequest;
import com.petbuddy.petbuddystore.dto.request.ReviewStatusUpdateRequest;
import com.petbuddy.petbuddystore.dto.response.OrderReviewResponse;
import com.petbuddy.petbuddystore.dto.response.ProductReviewManagerResponse;
import com.petbuddy.petbuddystore.dto.response.ProductReviewResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ReviewService {

    // ========================================
    // PRODUCT REVIEWS
    // ========================================

    /**
     * Tạo đánh giá sản phẩm
     */
    ProductReviewResponse createProductReview(
            String productId,
            ProductReviewCreationRequest request
    );

    /**
     * Lấy đánh giá của tôi cho sản phẩm
     */
    ProductReviewResponse getMyProductReview(String productId);

    /**
     * Lấy danh sách đánh giá của sản phẩm (phân trang, lọc theo rating)
     */
    Page<ProductReviewResponse> getProductReviews(
            String productId,
            Integer rating,
            Pageable pageable
    );

    /**
     * Cập nhật đánh giá của tôi (dùng chung cho PRODUCT và ORDER)
     */
    ProductReviewResponse updateReview(
            String reviewId,
            ProductReviewUpdateRequest request
    );

    /**
     * Xóa mềm đánh giá của tôi (dùng chung cho PRODUCT và ORDER)
     */
    void deleteReview(String reviewId);

    // ========================================
    // ORDER REVIEWS
    // ========================================

    /**
     * Tạo đánh giá đơn hàng
     */
    OrderReviewResponse createOrderReview(
            Long orderId,
            OrderReviewRequest request
    );

    /**
     * Lấy đánh giá của đơn hàng
     */
    OrderReviewResponse getOrderReview(Long orderId);

    // ========================================
    // MANAGEMENT
    // ========================================

    /**
     * Lấy danh sách đánh giá cho quản lý (có filter)
     */
    Page<ProductReviewManagerResponse> getAllReviews(
            String keyword,
            Integer rating,
            ReviewStatus status,
            String reviewType,
            Pageable pageable
    );

    /**
     * Lấy chi tiết đánh giá cho quản lý
     */
    ProductReviewManagerResponse getReviewDetailForManager(String reviewId);

    /**
     * Cập nhật trạng thái đánh giá (ACTIVE, HIDDEN)
     */
    void updateReviewStatus(
            String reviewId,
            ReviewStatusUpdateRequest request
    );
}