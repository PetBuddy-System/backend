package com.petbuddy.petbuddystore.service.impl;

import com.petbuddy.petbuddystore.common.enums.OrderStatus;
import com.petbuddy.petbuddystore.common.enums.ReviewStatus;
import com.petbuddy.petbuddystore.common.enums.ReviewType;
import com.petbuddy.petbuddystore.common.exception.AppException;
import com.petbuddy.petbuddystore.common.exception.ErrorCode;
import com.petbuddy.petbuddystore.dto.request.OrderReviewRequest;
import com.petbuddy.petbuddystore.dto.request.ProductReviewCreationRequest;
import com.petbuddy.petbuddystore.dto.request.ProductReviewUpdateRequest;
import com.petbuddy.petbuddystore.dto.request.ReviewStatusUpdateRequest;
import com.petbuddy.petbuddystore.dto.response.OrderReviewResponse;
import com.petbuddy.petbuddystore.dto.response.ProductReviewManagerResponse;
import com.petbuddy.petbuddystore.dto.response.ProductReviewResponse;
import com.petbuddy.petbuddystore.mapper.ReviewMapper;
import com.petbuddy.petbuddystore.model.Order;
import com.petbuddy.petbuddystore.model.Product;
import com.petbuddy.petbuddystore.model.Review;
import com.petbuddy.petbuddystore.model.User;
import com.petbuddy.petbuddystore.repository.ReviewRepository;
import com.petbuddy.petbuddystore.service.OrderService;
import com.petbuddy.petbuddystore.service.ProductService;
import com.petbuddy.petbuddystore.service.ReviewService;
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
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final ProductService productService;
    private final OrderService orderService;
    private final UserService userService;
    private final ReviewMapper reviewMapper;

    private static final int MAX_DAILY_REVIEWS = 10;

    // ========================================
    // PRODUCT REVIEWS
    // ========================================

    @Override
    @Transactional
    public ProductReviewResponse createProductReview(
            String productId,
            ProductReviewCreationRequest request
    ) {
        String userId = getCurrentUserId();
        log.info("Creating product review for user {} and product {}", userId, productId);

        UUID productUUID = UUID.fromString(productId);

        Product product = productService.getProductEntityById(productUUID);

        if (reviewRepository.hasActiveProductReview(userId, productUUID)) {
            throw new AppException(ErrorCode.REVIEW_ALREADY_EXISTS);
        }

        validateDailyLimit(userId);

        User user = userService.getUserEntityById(userId);

        Review review = reviewMapper.toEntity(request);
        review.setProduct(product);
        review.setUser(user);
        review.setReviewType(ReviewType.PRODUCT);
        review.setStatus(ReviewStatus.ACTIVE);

        Review savedReview = reviewRepository.save(review);
        log.info("Product review created successfully with id: {}", savedReview.getReviewId());

        updateProductRating(productUUID);

        return reviewMapper.toProductResponse(savedReview);
    }

    @Override
    public ProductReviewResponse getMyProductReview(String productId) {
        String userId = getCurrentUserId();
        log.info("Getting my product review for user {} and product {}", userId, productId);

        UUID productUUID = UUID.fromString(productId);

        return reviewRepository
                .findByUserUserIdAndProductProductIdAndReviewTypeAndStatusNot(
                        userId,
                        productUUID,
                        ReviewType.PRODUCT,
                        ReviewStatus.DELETED
                )
                .map(reviewMapper::toProductResponse)
                .orElse(null);
    }

    @Override
    public Page<ProductReviewResponse> getProductReviews(
            String productId,
            Integer rating,
            Pageable pageable
    ) {
        log.info("Getting product reviews for product: {}, rating: {}", productId, rating);

        UUID productUUID = UUID.fromString(productId);
        productService.getProductEntityById(productUUID);

        Page<Review> reviews = reviewRepository
                .findByProductProductIdAndReviewTypeAndStatusAndRating(
                        productUUID,
                        ReviewType.PRODUCT,
                        ReviewStatus.ACTIVE,
                        rating,
                        pageable
                );

        return reviews.map(reviewMapper::toProductResponse);
    }

    @Override
    @Transactional
    public ProductReviewResponse updateReview(
            String reviewId,
            ProductReviewUpdateRequest request
    ) {
        String userId = getCurrentUserId();
        log.info("Updating review {} by user {}", reviewId, userId);

        UUID reviewUUID = UUID.fromString(reviewId);

        Review review = reviewRepository
                .findByReviewIdAndStatusNot(reviewUUID, ReviewStatus.DELETED)
                .orElseThrow(() -> new AppException(ErrorCode.REVIEW_NOT_FOUND));

        if (!review.getUser().getUserId().equals(userId)) {
            throw new AppException(ErrorCode.UNAUTHORIZED_REVIEW_ACCESS);
        }

        reviewMapper.updateEntity(review, request);

        if (request.getAnonymous() != null) {
            review.setAnonymous(request.getAnonymous());
        }

        Review updatedReview = reviewRepository.save(review);
        log.info("Review updated successfully: {}", reviewId);

        if (review.getReviewType() == ReviewType.PRODUCT) {
            updateProductRating(review.getProduct().getProductId());
        }

        return reviewMapper.toProductResponse(updatedReview);
    }

    @Override
    @Transactional
    public void deleteReview(String reviewId) {
        String userId = getCurrentUserId();
        log.info("Deleting review {} by user {}", reviewId, userId);

        UUID reviewUUID = UUID.fromString(reviewId);

        Review review = reviewRepository
                .findByReviewIdAndStatusNot(reviewUUID, ReviewStatus.DELETED)
                .orElseThrow(() -> new AppException(ErrorCode.REVIEW_NOT_FOUND));

        if (!review.getUser().getUserId().equals(userId)) {
            throw new AppException(ErrorCode.UNAUTHORIZED_REVIEW_ACCESS);
        }

        review.setStatus(ReviewStatus.DELETED);
        review.setDeletedAt(LocalDateTime.now());
        reviewRepository.save(review);

        log.info("Review deleted successfully: {}", reviewId);

        if (review.getReviewType() == ReviewType.PRODUCT) {
            updateProductRating(review.getProduct().getProductId());
        }
    }

    // ========================================
    // ORDER REVIEWS
    // ========================================

    @Override
    @Transactional
    public OrderReviewResponse createOrderReview(
            Long orderId,
            OrderReviewRequest request
    ) {
        String userId = getCurrentUserId();
        log.info("Creating order review for user {} and order {}", userId, orderId);

        Order order = validateOrderReview(orderId, userId);

        validateDailyLimit(userId);

        User user = userService.getUserEntityById(userId);

        Review review = reviewMapper.toEntity(request);
        review.setOrder(order);
        review.setUser(user);
        review.setReviewType(ReviewType.ORDER);
        review.setStatus(ReviewStatus.ACTIVE);

        Review savedReview = reviewRepository.save(review);
        log.info("Order review created successfully with id: {}", savedReview.getReviewId());

        return reviewMapper.toOrderResponse(savedReview);
    }

    @Override
    public OrderReviewResponse getOrderReview(Long orderId) {
        String userId = getCurrentUserId();
        log.info("Getting order review for user {} and order {}", userId, orderId);

        // ✅ ĐÚNG: userId trước, orderId sau
        return reviewRepository
                .findByUserUserIdAndOrderOrderIdAndReviewTypeAndStatusNot(
                        userId,          // ← String
                        orderId,         // ← Long
                        ReviewType.ORDER,
                        ReviewStatus.DELETED
                )
                .map(reviewMapper::toOrderResponse)
                .orElse(null);
    }

    // ========================================
    // MANAGEMENT
    // ========================================

    @Override
    public Page<ProductReviewManagerResponse> getAllReviews(
            String keyword,
            Integer rating,
            ReviewStatus status,
            String reviewType,
            Pageable pageable
    ) {
        log.info("Getting all reviews - keyword: {}, rating: {}, status: {}, reviewType: {}",
                keyword, rating, status, reviewType);

        Specification<Review> spec = buildManagerFilterSpec(keyword, rating, status, reviewType);
        Page<Review> reviews = reviewRepository.findAll(spec, pageable);

        return reviews.map(reviewMapper::toManagerResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductReviewManagerResponse getReviewDetailForManager(String reviewId) {
        log.info("Getting review detail for manager: {}", reviewId);

        UUID reviewUUID = UUID.fromString(reviewId);

        Review review = reviewRepository
                .findByReviewIdAndStatusNot(reviewUUID, ReviewStatus.DELETED)
                .orElseThrow(() -> new AppException(ErrorCode.REVIEW_NOT_FOUND));

        return reviewMapper.toManagerDetailResponse(review);
    }

    @Override
    @Transactional
    public void updateReviewStatus(
            String reviewId,
            ReviewStatusUpdateRequest request
    ) {
        log.info("Updating review status: {} to {}", reviewId, request.getStatus());

        if (request.getStatus() == ReviewStatus.DELETED) {
            throw new AppException(ErrorCode.INVALID_OPERATION);
        }

        UUID reviewUUID = UUID.fromString(reviewId);

        Review review = reviewRepository
                .findByReviewIdAndStatusNot(reviewUUID, ReviewStatus.DELETED)
                .orElseThrow(() -> new AppException(ErrorCode.REVIEW_NOT_FOUND));

        review.setStatus(request.getStatus());
        reviewRepository.save(review);

        log.info("Review status updated successfully: {}", reviewId);

        if (review.getReviewType() == ReviewType.PRODUCT) {
            updateProductRating(review.getProduct().getProductId());
        }
    }

    // ========================================
    // PRIVATE METHODS
    // ========================================

    private Order validateOrderReview(Long orderId, String userId) {
        Order order = orderService.getOrderEntityById(orderId);

        if (!order.getUser().getUserId().equals(userId)) {
            throw new AppException(ErrorCode.UNAUTHORIZED_ORDER_ACCESS);
        }

        if (order.getStatus() != OrderStatus.COMPLETED) {
            throw new AppException(ErrorCode.ORDER_NOT_COMPLETED);
        }

        // ✅ ĐÚNG: orderId trước, userId sau
        if (reviewRepository.hasActiveOrderReview(orderId, userId)) {
            throw new AppException(ErrorCode.REVIEW_ALREADY_EXISTS);
        }

        return order;
    }

    private Specification<Review> buildManagerFilterSpec(
            String keyword,
            Integer rating,
            ReviewStatus status,
            String reviewType
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(cb.notEqual(root.get("status"), ReviewStatus.DELETED));

            ReviewType type = null;
            if (reviewType != null && !reviewType.isEmpty()) {
                try {
                    type = ReviewType.valueOf(reviewType.toUpperCase());
                    predicates.add(cb.equal(root.get("reviewType"), type));
                } catch (IllegalArgumentException e) {
                    throw new AppException(ErrorCode.INVALID_REVIEW_TYPE);
                }
            }

            if (keyword != null && !keyword.trim().isEmpty()) {
                String[] terms = keyword.trim().toLowerCase().split("\\s+");
                List<Predicate> keywordPredicates = new ArrayList<>();

                for (String term : terms) {
                    String searchTerm = "%" + term + "%";

                    if (type == null || type == ReviewType.PRODUCT) {
                        keywordPredicates.add(cb.or(
                                cb.like(cb.lower(root.get("product").get("name")), searchTerm),
                                cb.like(cb.lower(root.get("product").get("productCode")), searchTerm)
                        ));
                    }

                    if (type == null || type == ReviewType.ORDER) {
                        keywordPredicates.add(cb.like(cb.lower(root.get("order").get("orderCode")), searchTerm));
                    }

                    keywordPredicates.add(cb.like(cb.lower(root.get("user").get("fullName")), searchTerm));
                }

                if (!keywordPredicates.isEmpty()) {
                    if (keywordPredicates.size() == 1) {
                        predicates.add(keywordPredicates.get(0));
                    } else {
                        predicates.add(cb.or(keywordPredicates.toArray(new Predicate[0])));
                    }
                }
            }

            if (rating != null) {
                predicates.add(cb.equal(root.get("rating"), rating));
            }

            if (status != null && status != ReviewStatus.DELETED) {
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

    private void updateProductRating(UUID productId) {
        try {
            List<Review> reviews = reviewRepository
                    .findAllByProductProductIdAndReviewTypeAndStatusNotIn(
                            productId,
                            ReviewType.PRODUCT,
                            List.of(ReviewStatus.DELETED)
                    );

            if (!reviews.isEmpty()) {
                double averageRating = reviews.stream()
                        .filter(r -> r.getStatus() == ReviewStatus.ACTIVE)
                        .mapToInt(Review::getRating)
                        .average()
                        .orElse(0.0);

                long totalReviews = reviews.stream()
                        .filter(r -> r.getStatus() == ReviewStatus.ACTIVE)
                        .count();

                // TODO: Cập nhật vào Product entity
                log.info("Product {} - avg: {}, total: {}", productId, averageRating, totalReviews);
            }
        } catch (Exception e) {
            log.error("Failed to update product rating for product {}", productId, e);
        }
    }
}