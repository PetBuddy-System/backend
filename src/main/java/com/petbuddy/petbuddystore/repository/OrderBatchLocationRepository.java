package com.petbuddy.petbuddystore.repository;

import com.petbuddy.petbuddystore.model.OrderBatchLocation;
import com.petbuddy.petbuddystore.model.OrderDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface OrderBatchLocationRepository extends JpaRepository<OrderBatchLocation, Long> {
    List<OrderBatchLocation> findByOrderDetail_Order_OrderId(Long orderId);
    List<OrderBatchLocation> findByOrderDetail(OrderDetail orderDetail);
}
