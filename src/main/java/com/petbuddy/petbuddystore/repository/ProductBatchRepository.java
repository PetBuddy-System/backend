package com.petbuddy.petbuddystore.repository;

import com.petbuddy.petbuddystore.common.enums.ProductStatus;
import com.petbuddy.petbuddystore.model.ProductBatch;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductBatchRepository extends JpaRepository<ProductBatch, UUID>, JpaSpecificationExecutor<ProductBatch> {

    List<ProductBatch> findByStatusAndDeletedAtBefore(ProductStatus status,LocalDateTime deletedAt);

    boolean existsByProduct_ProductIdAndStatusIn(UUID productId,List<ProductStatus> statuses);

    Optional<ProductBatch> findByBatchCode(String batchCode);

    @Query("""
        SELECT COALESCE(SUM(b.stockQuantity), 0)
        FROM ProductBatch b
        WHERE b.product.productId = :productId
          AND b.status = com.petbuddy.petbuddystore.common.enums.ProductStatus.ACTIVE
          AND b.stockQuantity > 0
        """)
    int findAvailableStockByProductId(UUID productId);

    List<ProductBatch> findByProduct_ProductIdAndStockQuantityGreaterThanAndStatusOrderByExpiryDateAscCreatedAtAscBatchCodeAsc(
            UUID productId,
            Integer stockQuantity,
            ProductStatus status
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT b FROM ProductBatch b
        WHERE b.product.productId = :productId
          AND b.stockQuantity > 0
          AND b.status = :status
        ORDER BY b.expiryDate ASC, b.createdAt ASC, b.batchCode ASC
        """)
    List<ProductBatch> findActiveBatchesForUpdate(@Param("productId") UUID productId, @Param("status") ProductStatus status);

    @Query("""
    SELECT MAX(pb.basePrice)
    FROM ProductBatch pb
    WHERE pb.product.productId = :productId
      AND pb.status IN :statuses
""")
    BigDecimal findMaxBasePriceByProductId(@Param("productId") UUID productId, @Param("statuses") List<ProductStatus> statuses);

    List<ProductBatch> findByBatchCodeContaining(String batchCode);
}