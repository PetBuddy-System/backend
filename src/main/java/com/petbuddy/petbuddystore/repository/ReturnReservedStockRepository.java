package com.petbuddy.petbuddystore.repository;

import com.petbuddy.petbuddystore.model.ProductBatch;
import com.petbuddy.petbuddystore.model.ReturnItem;
import com.petbuddy.petbuddystore.model.ReturnRequest;
import com.petbuddy.petbuddystore.model.ReturnReservedStock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ReturnReservedStockRepository extends JpaRepository<ReturnReservedStock, Long> {

    @Query("SELECT rs FROM ReturnReservedStock rs " +
            "WHERE rs.returnItem.returnRequest = :returnRequest")
    List<ReturnReservedStock> findByReturnItem_ReturnRequest(@Param("returnRequest") ReturnRequest returnRequest);

    Optional<ReturnReservedStock> findByReturnItemAndBatch(ReturnItem returnItem, ProductBatch batch);
}