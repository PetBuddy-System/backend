package com.petbuddy.petbuddystore.service.impl;

import com.petbuddy.petbuddystore.common.enums.ReviewStatus;
import com.petbuddy.petbuddystore.common.exception.AppException;
import com.petbuddy.petbuddystore.common.exception.ErrorCode;
import com.petbuddy.petbuddystore.dto.request.ProductReviewCreationRequest;
import com.petbuddy.petbuddystore.dto.request.ProductReviewUpdateRequest;
import com.petbuddy.petbuddystore.dto.request.ReviewStatusUpdateRequest;
import com.petbuddy.petbuddystore.dto.response.ProductReviewManagerResponse;
import com.petbuddy.petbuddystore.dto.response.ProductReviewResponse;
import com.petbuddy.petbuddystore.mapper.ProductReviewMapper;
import com.petbuddy.petbuddystore.model.Product;
import com.petbuddy.petbuddystore.model.ProductReview;
import com.petbuddy.petbuddystore.model.User;
import com.petbuddy.petbuddystore.repository.ProductReviewRepository;
import com.petbuddy.petbuddystore.service.ProductReviewService;
import com.petbuddy.petbuddystore.service.ProductService;
import com.petbuddy.petbuddystore.service.UserService;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductReviewServiceImpl implements ProductReviewService {

    private final ProductReviewRepository reviewRepository;
    private final ProductService productService;
    private final UserService userService;
    private final ProductReviewMapper reviewMapper;

    private static final int MAX_DAILY_REVIEWS = 10;

    // ============ USER ============

    @Override
    @Transactional
    public ProductReviewResponse createReview(
            String productId,
            ProductReviewCreationRequest request
    ) {
        String userId = getCurrentUserId();
        log.info("Creating review for user {} and product {}", userId, productId);

        validateRating(request.getRating());
        validateContent(request.getContent());

        UUID productUUID = UUID.fromString(productId);
        Product product = productService.getProductEntityById(productUUID);
        User user = userService.getUserEntityById(userId);

        if (reviewRepository.existsByUserUserIdAndProductProductIdAndStatusNot(
                userId, productUUID, ReviewStatus.DELETED)) {
            throw new AppException(ErrorCode.REVIEW_ALREADY_EXISTS);
        }

        validateDailyLimit(userId);

        ProductReview review = reviewMapper.toEntity(request);
        review.setProduct(product);
        review.setUser(user);
        review.setStatus(ReviewStatus.ACTIVE);
        review.setAnonymous(request.getAnonymous() != null && request.getAnonymous());

        ProductReview savedReview = reviewRepository.save(review);
        log.info("Review created successfully with id: {}", savedReview.getReviewId());

        return reviewMapper.toResponse(savedReview);
    }

    @Override
    @Transactional
    public ProductReviewResponse updateReview(
            String reviewId,
            ProductReviewUpdateRequest request
    ) {
        String userId = getCurrentUserId();
        log.info("Updating review {} by user {}", reviewId, userId);

        validateRating(request.getRating());
        validateContent(request.getContent());

        UUID reviewUUID = UUID.fromString(reviewId);

        ProductReview review = reviewRepository.findByReviewIdAndStatusNot(
                        reviewUUID, ReviewStatus.DELETED)
                .orElseThrow(() -> new AppException(ErrorCode.REVIEW_NOT_FOUND));

        if (!review.getUser().getUserId().equals(userId)) {
            throw new AppException(ErrorCode.UNAUTHORIZED_REVIEW_ACCESS);
        }

        reviewMapper.updateEntity(review, request);
        if (request.getAnonymous() != null) {
            review.setAnonymous(request.getAnonymous());
        }

        ProductReview updatedReview = reviewRepository.save(review);
        log.info("Review updated successfully: {}", reviewId);

        return reviewMapper.toResponse(updatedReview);
    }

    @Override
    @Transactional
    public void deleteReview(String reviewId) {
        String userId = getCurrentUserId();
        log.info("Deleting review {} by user {}", reviewId, userId);

        UUID reviewUUID = UUID.fromString(reviewId);

        ProductReview review = reviewRepository.findByReviewIdAndStatusNot(
                        reviewUUID, ReviewStatus.DELETED)
                .orElseThrow(() -> new AppException(ErrorCode.REVIEW_NOT_FOUND));

        if (!review.getUser().getUserId().equals(userId)) {
            throw new AppException(ErrorCode.UNAUTHORIZED_REVIEW_ACCESS);
        }

        review.setStatus(ReviewStatus.DELETED);
        review.setDeletedAt(LocalDateTime.now());
        reviewRepository.save(review);

        log.info("Review deleted successfully: {}", reviewId);
    }

    @Override
    public ProductReviewResponse getMyReview(String productId) {
        String userId = getCurrentUserId();
        log.info("Getting my review for user {} and product {}", userId, productId);

        UUID productUUID = UUID.fromString(productId);

        Optional<ProductReview> reviewOptional = reviewRepository
                .findByUserUserIdAndProductProductIdAndStatusNot(
                        userId, productUUID, ReviewStatus.DELETED);

        return reviewOptional.map(reviewMapper::toResponse).orElse(null);
    }

    @Override
    public Page<ProductReviewResponse> getProductReviews(
            String productId,
            Integer rating,
            Pageable pageable
    ) {
        log.info("Getting reviews for product: {}, rating: {}", productId, rating);

        UUID productUUID = UUID.fromString(productId);
        productService.getProductEntityById(productUUID);

        Page<ProductReview> reviews = reviewRepository
                .findByProductProductIdAndStatusAndRating(
                        productUUID, ReviewStatus.ACTIVE, rating, pageable);

        return reviews.map(reviewMapper::toResponse);
    }

    // ============ MANAGER/ADMIN ============

    @Override
    public Page<ProductReviewManagerResponse> getAllReviews(
            String keyword,
            Integer rating,
            ReviewStatus status,
            Pageable pageable
    ) {
        log.info("Getting all reviews with keyword: {}, rating: {}, status: {}", keyword, rating, status);

        Specification<ProductReview> spec = buildManagerFilterSpec(keyword, rating, status);
        Page<ProductReview> reviews = reviewRepository.findAll(spec, pageable);

        return reviews.map(reviewMapper::toManagerResponse);
    }

    @Override
    @Transactional
    public void updateReviewStatus(String reviewId, ReviewStatusUpdateRequest request) {
        log.info("Updating review status: {} to {}", reviewId, request.getStatus());

        if (request.getStatus() == ReviewStatus.DELETED) {
            throw new AppException(ErrorCode.REVIEW_ALREADY_DELETED);
        }

        UUID reviewUUID = UUID.fromString(reviewId);

        ProductReview review = reviewRepository.findByReviewIdAndStatusNot(
                        reviewUUID, ReviewStatus.DELETED)
                .orElseThrow(() -> new AppException(ErrorCode.REVIEW_NOT_FOUND));

        review.setStatus(request.getStatus());
        reviewRepository.save(review);

        log.info("Review status updated successfully: {}", reviewId);
    }

    // ============ PRIVATE METHODS ============

    private Specification<ProductReview> buildManagerFilterSpec(
            String keyword,
            Integer rating,
            ReviewStatus status
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 1. Loại trừ DELETED
            predicates.add(cb.notEqual(root.get("status"), ReviewStatus.DELETED));

            // 2. Filter theo keyword (user.fullName, product.name, product.productCode)
            if (keyword != null && !keyword.isBlank()) {
                String[] terms = keyword.trim().toLowerCase().split("\\s+");
                List<Predicate> keywordPredicates = new ArrayList<>();

                for (String term : terms) {
                    String searchTerm = "%" + term + "%";
                    keywordPredicates.add(cb.or(
                            cb.like(cb.lower(root.get("user").get("fullName")), searchTerm),
                            cb.like(cb.lower(root.get("product").get("name")), searchTerm),
                            cb.like(cb.lower(root.get("product").get("productCode")), searchTerm)
                    ));
                }

                if (keywordPredicates.size() == 1) {
                    predicates.add(keywordPredicates.get(0));
                } else {
                    predicates.add(cb.and(keywordPredicates.toArray(new Predicate[0])));
                }
            }

            // 3. Filter theo rating
            if (rating != null) {
                predicates.add(cb.equal(root.get("rating"), rating));
            }

            // 4. Filter theo status (ACTIVE, HIDDEN)
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private String getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }
        return authentication.getName();
    }

    private void validateRating(Integer rating) {
        if (rating == null || rating < 1 || rating > 5) {
            throw new AppException(ErrorCode.INVALID_RATING);
        }
    }

    private void validateContent(String content) {
        if (content == null || content.trim().isEmpty()) {
            throw new AppException(ErrorCode.INVALID_CONTENT);
        }
        if (content.length() > 1000) {
            throw new AppException(ErrorCode.CONTENT_TOO_LONG);
        }
    }

    private void validateDailyLimit(String userId) {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = LocalDate.now().plusDays(1).atStartOfDay();

        long count = reviewRepository.countByUserUserIdAndCreatedAtBetweenAndStatusNot(
                userId,
                startOfDay,
                endOfDay,
                ReviewStatus.DELETED
        );

        if (count >= MAX_DAILY_REVIEWS) {
            throw new AppException(ErrorCode.REVIEW_DAILY_LIMIT_EXCEEDED);
        }
    }
}