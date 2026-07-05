package com.petbuddy.petbuddystore.repository;

import com.petbuddy.petbuddystore.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByOrder_OrderId(Long orderId);

    Optional<Payment> findByStripePaymentIntentId(String stripePaymentIntentId);

    boolean existsByOrder_OrderId(Long orderId);

    @Query("""
        select coalesce(sum(p.amount), 0)
        from Payment p
        where p.status = 'PAID' and p.paidAt between :start and :end
    """)
    BigDecimal sumRevenue(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("""
        select count(p)
        from Payment p
        where p.status = 'PAID' and p.paidAt between :start and :end
    """)
    Long countSuccessfulPayments(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("""
        select count(p)
        from Payment p
        where p.createdAt between :start and :end
    """)
    Long countTotalPayments(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    // Trend theo ngày (DB-agnostic bằng function('date', ...) nếu dùng Postgres/MySQL)
    @Query("""
        select function('date', p.paidAt) as day, coalesce(sum(p.amount), 0) as revenue
        from Payment p
        where p.status = 'PAID' and p.paidAt between :start and :end
        group by function('date', p.paidAt)
        order by day
    """)
    List<Object[]> sumRevenueGroupedByDay(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}
