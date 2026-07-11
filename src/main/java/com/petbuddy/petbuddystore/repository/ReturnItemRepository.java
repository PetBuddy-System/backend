package com.petbuddy.petbuddystore.repository;

import com.petbuddy.petbuddystore.common.enums.ReturnStatus;
import com.petbuddy.petbuddystore.model.ReturnItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;

@Repository
public interface ReturnItemRepository extends JpaRepository<ReturnItem, Long> {

    @Query("SELECT COALESCE(SUM(ri.quantity), 0) FROM ReturnItem ri " +
           "WHERE ri.orderDetail.orderDetailId = :orderDetailId " +
           "AND ri.returnRequest.status IN :statuses")
    int sumQuantityByOrderDetailIdAndStatusIn(
            @Param("orderDetailId") Long orderDetailId,
            @Param("statuses") Collection<ReturnStatus> statuses);
}
