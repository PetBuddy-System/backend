package com.petbuddy.petbuddystore.repository;

import com.petbuddy.petbuddystore.model.OrderBatchLocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderBatchLocationRepository extends JpaRepository<OrderBatchLocation, Long> {
    List<OrderBatchLocation> findByOrderDetail_Order_OrderId(Long orderId);
}
