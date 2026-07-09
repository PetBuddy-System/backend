package com.petbuddy.petbuddystore.repository;

import com.petbuddy.petbuddystore.model.OrderDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.UUID;

@Repository
public interface OrderDetailRepository extends JpaRepository<OrderDetail, Long> {
    @Query("""
            SELECT COALESCE(SUM(od.quantity), 0)
            FROM OrderDetail od
            WHERE od.product.productId = :productId
              AND od.order.status = 'PENDING'
              AND od.order.paymentExpiredAt > :now
              AND od.order.payment.paymentMethod <> 'CARD'
            """)
    Integer sumHeldQuantityByProductId(@Param("productId") UUID productId,
                                       @Param("now") LocalDateTime now);
}
