package com.petbuddy.petbuddystore.repository;

import com.petbuddy.petbuddystore.common.enums.OrderStatus;
import com.petbuddy.petbuddystore.common.enums.PaymentMethod;
import com.petbuddy.petbuddystore.model.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    Page<Order> findByUser_UserIdOrderByCreatedAtDesc(String userId, Pageable pageable);
    List<Order> findByStatusAndPaymentExpiredAtBeforeAndPayment_PaymentMethodIn(
            OrderStatus status, LocalDateTime dateTime, List<PaymentMethod> methods);
    long countByCreatedAtBetween(LocalDateTime start, LocalDateTime end);
    Page<Order> findByStaffSchedule_Staff_UserId(String userId, Pageable pageable);
    boolean existsByUserUserIdAndOrderDetailsProductProductIdAndStatus(String userId, UUID productId, OrderStatus status);
    List<Order> findByStatusAndLatitudeIsNotNullAndLongitudeIsNotNull(OrderStatus status);
    long countByStaffSchedule_StaffScheduleIdAndStatusIn(String staffScheduleId, List<OrderStatus> statuses);
}

