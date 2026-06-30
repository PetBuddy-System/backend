package com.petbuddy.petbuddystore.repository;

import com.petbuddy.petbuddystore.common.enums.PromotionStatus;
import com.petbuddy.petbuddystore.model.PromotionDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PromotionDetailRepository extends JpaRepository<PromotionDetail, UUID> {

    boolean existsByProduct_ProductIdAndPromotion_Status(UUID productId, PromotionStatus status);

    @Query("SELECT pd FROM PromotionDetail pd " + "JOIN pd.promotion p " + "WHERE pd.product.productId = :productId " + "AND p.status = :status " + "AND p.deletedAt IS NULL")
    Optional<PromotionDetail> findByProduct_ProductIdAndPromotion_Status(@Param("productId") UUID productId, @Param("status") PromotionStatus status);
}
