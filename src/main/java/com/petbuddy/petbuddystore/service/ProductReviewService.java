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

    ProductReviewResponse createReview(String productId, ProductReviewCreationRequest request);

    ProductReviewResponse updateReview(String reviewId, ProductReviewUpdateRequest request);

    void deleteReview(String reviewId);

    ProductReviewResponse getMyReview(String productId);

    Page<ProductReviewResponse> getProductReviews(String productId, Integer rating, Pageable pageable);

    Page<ProductReviewManagerResponse> getAllReviews(String keyword, Integer rating, ReviewStatus status, Pageable pageable);

    ProductReviewManagerResponse getReviewDetailForManager(String reviewId);

    void updateReviewStatus(String reviewId, ReviewStatusUpdateRequest request);
}