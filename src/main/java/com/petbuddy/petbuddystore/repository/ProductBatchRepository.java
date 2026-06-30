package com.petbuddy.petbuddystore.repository;

import com.petbuddy.petbuddystore.common.enums.ProductStatus;
import com.petbuddy.petbuddystore.model.ProductBatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface ProductBatchRepository extends JpaRepository<ProductBatch, UUID>, JpaSpecificationExecutor<ProductBatch> {

    long countByProduct_ProductId(UUID productId);

    boolean existsByBatchCode(String batchCode);

    List<ProductBatch> findByStatusAndDeletedAtBefore(ProductStatus status,LocalDateTime deletedAt);

    boolean existsByProduct_ProductIdAndStatusIn(UUID productId,List<ProductStatus> statuses);

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

    List<ProductBatch> findByProduct_ProductIdInAndStatusAndDeletedAtIsNull(
            List<UUID> productIds,
            ProductStatus status
    );
}