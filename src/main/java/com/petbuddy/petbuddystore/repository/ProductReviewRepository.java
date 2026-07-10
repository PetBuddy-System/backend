package com.petbuddy.petbuddystore.repository;

import com.petbuddy.petbuddystore.common.enums.ReviewStatus;
import com.petbuddy.petbuddystore.model.ProductReview;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductReviewRepository extends JpaRepository<ProductReview, UUID>, JpaSpecificationExecutor<ProductReview> {

    // ============ USER ============

    /**
     * Kiểm tra user đã review product chưa (không bao gồm DELETED)
     */
    boolean existsByUserUserIdAndProductProductIdAndStatusNot(
            String userId,
            UUID productId,
            ReviewStatus status
    );

    /**
     * Lấy review của user cho product cụ thể (không bao gồm DELETED)
     */
    Optional<ProductReview> findByUserUserIdAndProductProductIdAndStatusNot(
            String userId,
            UUID productId,
            ReviewStatus status
    );

    /**
     * Lấy review theo ID và không bị xóa
     */
    Optional<ProductReview> findByReviewIdAndStatusNot(
            UUID reviewId,
            ReviewStatus status
    );

    /**
     * Lấy danh sách review của product (phân trang, ACTIVE, có lọc rating)
     */
    @Query("SELECT r FROM ProductReview r " +
            "WHERE r.product.productId = :productId " +
            "AND r.status = :status " +
            "AND (:rating IS NULL OR r.rating = :rating)")
    Page<ProductReview> findByProductProductIdAndStatusAndRating(
            @Param("productId") UUID productId,
            @Param("status") ReviewStatus status,
            @Param("rating") Integer rating,
            Pageable pageable
    );

    /**
     * Đếm số review của user trong khoảng thời gian (không bao gồm DELETED)
     */
    long countByUserUserIdAndCreatedAtBetweenAndStatusNot(
            String userId,
            LocalDateTime startDateTime,
            LocalDateTime endDateTime,
            ReviewStatus status
    );
}