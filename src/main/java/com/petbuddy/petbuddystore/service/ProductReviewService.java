package com.petbuddy.petbuddystore.service;

import com.petbuddy.petbuddystore.common.enums.ReviewStatus;
import com.petbuddy.petbuddystore.dto.request.ProductReviewCreationRequest;
import com.petbuddy.petbuddystore.dto.request.ProductReviewUpdateRequest;
import com.petbuddy.petbuddystore.dto.request.ReviewStatusUpdateRequest;
import com.petbuddy.petbuddystore.dto.response.ProductReviewManagerResponse;
import com.petbuddy.petbuddystore.dto.response.ProductReviewResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ProductReviewService {

    // ============ USER ============

    /**
     * Tạo review mới
     */
    ProductReviewResponse createReview(String productId, ProductReviewCreationRequest request);

    /**
     * Cập nhật review của tôi
     */
    ProductReviewResponse updateReview(String reviewId, ProductReviewUpdateRequest request);

    /**
     * Xóa mềm review của tôi
     */
    void deleteReview(String reviewId);

    /**
     * Lấy review của tôi cho sản phẩm
     */
    ProductReviewResponse getMyReview(String productId);

    /**
     * Lấy danh sách review của sản phẩm (có lọc theo rating)
     */
    Page<ProductReviewResponse> getProductReviews(
            String productId,
            Integer rating,
            Pageable pageable
    );

    // ============ MANAGER/ADMIN ============

    /**
     * Lấy tất cả review (có filter: keyword, rating, status)
     */
    Page<ProductReviewManagerResponse> getAllReviews(
            String keyword,
            Integer rating,
            ReviewStatus status,
            Pageable pageable
    );

    /**
     * Cập nhật status review (ACTIVE, HIDDEN)
     */
    void updateReviewStatus(String reviewId, ReviewStatusUpdateRequest request);
}