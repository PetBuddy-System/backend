package com.petbuddy.petbuddystore.repository;

import com.petbuddy.petbuddystore.common.enums.ReviewStatus;
import com.petbuddy.petbuddystore.common.enums.ReviewType;
import com.petbuddy.petbuddystore.model.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReviewRepository extends JpaRepository<Review, UUID>, JpaSpecificationExecutor<Review> {

    // ============ PRODUCT REVIEWS ============

    default boolean hasActiveProductReview(String userId, UUID productId) {
        return existsByUserUserIdAndProductProductIdAndReviewTypeAndStatusIn(
                userId,
                productId,
                ReviewType.PRODUCT,
                List.of(ReviewStatus.ACTIVE, ReviewStatus.HIDDEN)
        );
    }

    boolean existsByUserUserIdAndProductProductIdAndReviewTypeAndStatusIn(
            String userId,
            UUID productId,
            ReviewType reviewType,
            List<ReviewStatus> statuses
    );

    Optional<Review> findByUserUserIdAndProductProductIdAndReviewTypeAndStatusNot(
            String userId,
            UUID productId,
            ReviewType reviewType,
            ReviewStatus excludedStatus
    );

    // ============ ORDER REVIEWS ============

    /**
     * Kiểm tra user có review order ACTIVE/HIDDEN không
     */
    default boolean hasActiveOrderReview(Long orderId, String userId) {
        return existsByUserUserIdAndOrderOrderIdAndReviewTypeAndStatusIn(
                userId,          // ← String (khớp với UserUserId)
                orderId,         // ← Long (khớp với OrderOrderId)
                ReviewType.ORDER,
                List.of(ReviewStatus.ACTIVE, ReviewStatus.HIDDEN)
        );
    }


    boolean existsByUserUserIdAndOrderOrderIdAndReviewTypeAndStatusIn(
            String userId,
            Long orderId,
            ReviewType reviewType,
            List<ReviewStatus> statuses
    );

    Optional<Review> findByUserUserIdAndOrderOrderIdAndReviewTypeAndStatusNot(
            String userId,
            Long orderId,
            ReviewType reviewType,
            ReviewStatus excludedStatus
    );

    // ============ GENERAL QUERIES ============

    Optional<Review> findByReviewIdAndStatusNot(
            UUID reviewId,
            ReviewStatus excludedStatus
    );

    @Query("""
            SELECT r
            FROM Review r
            WHERE r.product.productId = :productId
              AND r.reviewType = :reviewType
              AND r.status = :status
              AND (:rating IS NULL OR r.rating = :rating)
            ORDER BY r.createdAt DESC
    """)
    Page<Review> findByProductProductIdAndReviewTypeAndStatusAndRating(
            @Param("productId") UUID productId,
            @Param("reviewType") ReviewType reviewType,
            @Param("status") ReviewStatus status,
            @Param("rating") Integer rating,
            Pageable pageable
    );

    long countByUserUserIdAndCreatedAtBetweenAndStatusNot(
            String userId,
            LocalDateTime startDateTime,
            LocalDateTime endDateTime,
            ReviewStatus excludedStatus
    );

    @Query("""
            SELECT r
            FROM Review r
            WHERE r.product.productId = :productId
              AND r.reviewType = :reviewType
              AND r.status NOT IN :excludedStatuses
    """)
    List<Review> findAllByProductProductIdAndReviewTypeAndStatusNotIn(
            @Param("productId") UUID productId,
            @Param("reviewType") ReviewType reviewType,
            @Param("excludedStatuses") List<ReviewStatus> excludedStatuses
    );
}